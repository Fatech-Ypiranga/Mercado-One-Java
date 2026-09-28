import { expect, test, type Page, type Route } from '@playwright/test';

const loginName = process.env['MERCADO_ONE_E2E_LOGIN'] ?? 'e2e-admin';
const password = process.env['MERCADO_ONE_E2E_PASSWORD'] ?? 'e2e-password';
const now = '2026-09-28T12:00:00Z';
type ApiHandler = (route: Route) => Promise<boolean>;

const envelope = (data: unknown) => ({ success: true, data, error: null, timestamp: now });

async function json(route: Route, data: unknown) {
  const origin = route.request().headers()['origin'] ?? 'http://127.0.0.1:4200';
  await route.fulfill({
    status: 200, contentType: 'application/json',
    headers: {
      'access-control-allow-origin': origin,
      'access-control-allow-credentials': 'true',
      'access-control-allow-headers': 'content-type',
      'access-control-allow-methods': 'GET, POST, PUT, OPTIONS',
    },
    body: JSON.stringify(data),
  });
}

async function setupApi(page: Page, handler?: ApiHandler) {
  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (request.method() === 'OPTIONS') await json(route, {});
    else if (handler && await handler(route)) return;
    else if (path === '/api/auth/login') await json(route, envelope({
      accessToken: 'browser-test-token', tokenType: 'Bearer',
      expiresAt: '2099-01-01T00:00:00Z',
      user: { id: 1, nome: 'Admin de teste', login: loginName, perfil: 'ADMIN' },
    }));
    else if (path === '/api/system/info') await json(route, envelope({ name: 'Mercado One', version: 'e2e' }));
    else if (path === '/api/offline/sales/conflicts') await json(route, envelope([]));
    else if (path === '/api/sales') await json(route, envelope({
      items: [], totals: { saleCount: 0, totalAmount: 0, totalItems: 0, totalsByPaymentMethod: {} },
      page: { page: 0, size: 20, totalElements: 0, totalPages: 0 },
    }));
    else await json(route, envelope([]));
  });
}

async function signIn(page: Page) {
  await page.goto('/login');
  await page.locator('input[formcontrolname="login"]').fill(loginName);
  await page.locator('input[formcontrolname="password"]').fill(password);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page).toHaveURL(/\/$/);
}

test('login and navigation work at desktop and mobile widths', async ({ page }) => {
  await setupApi(page);
  await signIn(page);
  await expect(page.getByRole('heading', { name: /início/i })).toBeVisible();
  await expect(page.getByText(/R\$\s*0,00/).first()).toBeVisible();
});

test('stock clerk sees only authorized navigation and routes', async ({ page, isMobile }) => {
  await setupApi(page);
  await page.addInitScript(() => sessionStorage.setItem('mercado-one-admin-session-state', JSON.stringify({
    expiresAt: '2099-01-01T00:00:00Z',
    user: { id: 3, nome: 'Estoquista de teste', login: 'estoquista', perfil: 'ESTOQUISTA' },
  })));
  await page.goto('/');
  if (isMobile) await page.getByRole('button', { name: 'Menu', exact: true }).click();
  const navigation = page.getByRole('navigation', { name: 'Áreas do administrativo' });
  await expect(navigation.getByRole('link', { name: 'Estoque' })).toBeVisible();
  await expect(navigation.getByRole('link', { name: 'Produtos' })).toHaveCount(0);
  await expect(navigation.getByRole('link', { name: 'Usuários' })).toHaveCount(0);
  await page.goto('/usuarios');
  await expect(page).toHaveURL(/\/$/);
});

test('category can be created from the admin interface', async ({ page, isMobile }) => {
  let categories: unknown[] = [];
  let submitted: unknown;
  await setupApi(page, async (route) => {
    if (new URL(route.request().url()).pathname !== '/api/catalog/categories') return false;
    if (route.request().method() === 'POST') {
      submitted = route.request().postDataJSON();
      categories = [{ id: 7, name: 'Hortifruti', active: true, createdAt: now, updatedAt: now }];
      await json(route, envelope(categories[0]));
    } else await json(route, envelope(categories));
    return true;
  });
  await signIn(page);
  await page.goto('/categorias');
  if (isMobile) {
    const newButton = page.getByRole('button', { name: 'Nova', exact: true });
    await expect(newButton).toBeVisible();
    await newButton.click();
  }
  await page.locator('input[formcontrolname="name"]').fill('Hortifruti');
  await page.getByRole('button', { name: 'Salvar', exact: true }).click();
  await expect(page.getByText('Hortifruti')).toBeVisible();
  expect(submitted).toMatchObject({ name: 'Hortifruti', active: true });
});

test('stock entry submits the selected product and quantity', async ({ page }) => {
  const product = {
    id: 11, name: 'Arroz 1 kg', sku: 'ARROZ-1', barcode: null,
    category: { id: 2, name: 'Mercearia', active: true }, unit: 'UN',
    salePrice: 8, active: true, ncm: null, cest: null, defaultCfop: null,
    merchandiseOrigin: null, taxClassification: null, createdAt: now, updatedAt: now,
  };
  let submitted: Record<string, unknown> | undefined;
  await setupApi(page, async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/api/catalog/products') await json(route, envelope([product]));
    else if (path === '/api/suppliers') await json(route, envelope([]));
    else if (path === '/api/inventory/balances') await json(route, envelope([]));
    else if (path === '/api/inventory/movements') await json(route, envelope([]));
    else if (path === '/api/inventory/entries' && request.method() === 'POST') {
      submitted = request.postDataJSON() as Record<string, unknown>;
      await json(route, envelope({ id: 1, product, type: 'ENTRY', quantityDelta: 3, quantityBefore: 0, quantityAfter: 3, reason: 'Entrada', supplierName: null, supplier: null, documentNumber: null, note: null, createdByUserId: 1, createdAt: now }));
    } else return false;
    return true;
  });
  await signIn(page);
  await page.goto('/estoque');
  const newButton = page.getByRole('button', { name: /nova movimentação/i });
  if (await newButton.isVisible()) await newButton.click();
  await page.locator('select[formcontrolname="productId"]').selectOption({ label: 'Arroz 1 kg - ARROZ-1' });
  await page.locator('input[formcontrolname="quantity"]').fill('3');
  await page.getByRole('button', { name: 'Registrar', exact: true }).click();
  await expect.poll(() => submitted).toMatchObject({ productId: 11, quantity: 3 });
});

test('offline conflict requires confirmation before resolution', async ({ page }) => {
  const conflict = {
    id: 10, localSaleId: 'local-e2e-1', createdAt: now, customerId: null,
    operatorUserId: 2, status: 'PENDING', conflictSummary: 'Preço alterado',
    salePayload: JSON.stringify({ localSaleId: 'local-e2e-1', createdAt: now, customerId: null, items: [{ productId: 11, quantity: 1, unitPrice: 8 }], payments: [{ method: 'CASH', amount: 8 }] }),
    remoteSaleId: null, resolutionNote: null, resolvedByUserId: null,
    resolvedAt: null, updatedAt: now,
  };
  let pending = true;
  let resolution: unknown;
  await setupApi(page, async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/api/offline/sales/conflicts') await json(route, envelope(pending ? [conflict] : []));
    else if (path === '/api/offline/sales/conflicts/10/resolve') {
      resolution = request.postDataJSON();
      pending = false;
      await json(route, envelope({ ...conflict, status: 'ACCEPTED', remoteSaleId: 20 }));
    } else return false;
    return true;
  });
  await signIn(page);
  await page.goto('/offline');
  await expect(page.getByText('local-e2e-1').first()).toBeVisible();
  await page.getByRole('button', { name: 'Aceitar', exact: true }).click();
  expect(resolution).toBeUndefined();
  await page.getByRole('button', { name: 'Confirmar aceite' }).click();
  await expect.poll(() => resolution).toMatchObject({ action: 'ACCEPT', note: null });
});
