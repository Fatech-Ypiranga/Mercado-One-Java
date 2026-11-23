// Roteiro integrado do MVP contra a API real (PostgreSQL). Uso: node infra/verificacao/smoke-mvp.mjs [baseUrl]
// ATENCAO: cria usuarios, produtos e vendas de teste. Rode só contra um banco descartável (ver README.md desta pasta).
const BASE = process.argv[2] || 'http://localhost:8080';
const run = Date.now().toString(36);
let pass = 0, fail = 0;
const failures = [];

async function api(method, path, body, token) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'content-type': 'application/json', ...(token ? { authorization: 'Bearer ' + token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let json = null; try { json = JSON.parse(text); } catch { /* csv */ }
  return { status: res.status, json, data: json && json.data, error: json && json.error, text };
}
function check(name, ok, detail = '') {
  if (ok) { pass++; console.log('  ok   ' + name); }
  else { fail++; failures.push(name + (detail ? ' -> ' + detail : '')); console.log('  FALHA ' + name + (detail ? ' -> ' + detail : '')); }
}
const section = (t) => console.log('\n# ' + t);
const num = (v) => Number(v);
async function login(l, p) { const r = await api('POST', '/api/auth/login', { login: l, password: p }); return r; }

(async () => {
  section('Saúde e login');
  const h = await fetch(BASE + '/actuator/health'); const hj = await h.json();
  check('actuator/health = UP', hj.status === 'UP', JSON.stringify(hj));
  const a = await login('admin', 'admin123');
  check('login do admin seed', a.status === 200 && a.data && a.data.accessToken, 'status ' + a.status);
  const A = a.data.accessToken;
  const me = await api('GET', '/api/auth/me', undefined, A);
  check('/auth/me devolve o perfil ADMIN', me.data && me.data.perfil === 'ADMIN');
  const bad = await login('admin', 'senha-errada');
  check('senha errada -> 401 INVALID_CREDENTIALS', bad.status === 401 && bad.error && bad.error.code === 'INVALID_CREDENTIALS', 'status ' + bad.status);

  section('UC01 Usuários e permissões');
  const mk = async (nome, login, perfil, active = true) => api('POST', '/api/access/users', { nome, login: `${login}-${run}`, password: 'senha123', perfil, active }, A);
  const gU = await mk('Gerente Teste', 'gerente', 'GERENTE');
  const oU = await mk('Operador Teste', 'operador', 'OPERADOR_CAIXA');
  const eU = await mk('Estoquista Teste', 'estoquista', 'ESTOQUISTA');
  const iU = await mk('Inativo Teste', 'inativo', 'OPERADOR_CAIXA', false);
  check('admin cria 4 usuários', [gU, oU, eU, iU].every((r) => r.status === 200 || r.status === 201), [gU, oU, eU, iU].map((r) => r.status).join(','));
  const G = (await login(`gerente-${run}`, 'senha123')).data.accessToken;
  const O = (await login(`operador-${run}`, 'senha123')).data.accessToken;
  const E = (await login(`estoquista-${run}`, 'senha123')).data.accessToken;
  const inat = await login(`inativo-${run}`, 'senha123');
  check('usuário inativo não autentica', inat.status === 401, 'status ' + inat.status);
  const dup = await mk('Dup', 'gerente', 'GERENTE');
  check('login duplicado é recusado', dup.status >= 400 && dup.status < 500, 'status ' + dup.status);
  check('operador não gerencia usuários (403)', (await api('POST', '/api/access/users', { nome: 'x', login: 'x-' + run, password: 'senha123', perfil: 'GERENTE', active: true }, O)).status === 403);
  check('estoquista não vende (403)', (await api('POST', '/api/sales', { items: [], payments: [] }, E)).status === 403);
  check('operador não vê relatório de vendas (403)', (await api('GET', '/api/sales', undefined, O)).status === 403);
  check('auditoria só para ADMIN (403 ao gerente)', (await api('GET', '/api/audit/events', undefined, G)).status === 403);
  check('sem token -> 401', (await api('GET', '/api/catalog/products')).status === 401);

  section('UC02 Cadastros: categoria, fornecedor, cliente, produto');
  const cat = await api('POST', '/api/catalog/categories', { name: `Mercearia ${run}`, active: true }, G);
  check('gerente cria categoria', cat.status < 300 && cat.data && cat.data.id, 'status ' + cat.status);
  const catId = cat.data.id;
  const sup = await api('POST', '/api/suppliers', { name: `Fornecedor ${run}`, document: null, phone: '1133334444', email: 'f@exemplo.com', notes: 'teste', active: true }, G);
  check('gerente cria fornecedor', sup.status < 300 && sup.data && sup.data.id, 'status ' + sup.status + ' ' + JSON.stringify(sup.error));
  const cust = await api('POST', '/api/customers', { name: `Maria ${run}`, phone: '11999990000', email: 'm@exemplo.com', document: null, contactConsent: true, active: true }, G);
  check('gerente cria cliente com consentimento', cust.status < 300 && cust.data && cust.data.id, 'status ' + cust.status);
  const barcode = '789' + Date.now().toString().slice(-10);
  const prodBody = { name: `Arroz ${run}`, barcode, sku: `ARROZ-${run}`, categoryId: catId, unit: 'UN', salePrice: 10.00, active: true, ncm: '10063021', cest: null, defaultCfop: '5102', merchandiseOrigin: '0', taxClassification: null };
  const prod = await api('POST', '/api/catalog/products', prodBody, G);
  check('gerente cria produto com dados fiscais', prod.status < 300 && prod.data && prod.data.id, 'status ' + prod.status + ' ' + JSON.stringify(prod.error));
  const P = prod.data.id;
  const prodOff = await api('POST', '/api/catalog/products', { ...prodBody, name: `Inativo ${run}`, barcode: barcode + '1', sku: `INAT-${run}`, active: false }, G);
  const PO = prodOff.data && prodOff.data.id;
  const s1 = await api('GET', `/api/catalog/products?search=${encodeURIComponent('Arroz ' + run)}`, undefined, G);
  const s2 = await api('GET', `/api/catalog/products?search=ARROZ-${run}`, undefined, G);
  const s3 = await api('GET', `/api/catalog/products?search=${barcode}`, undefined, G);
  check('busca de produto por nome, SKU e código de barras', [s1, s2, s3].every((r) => Array.isArray(r.data) && r.data.some((p) => p.id === P)));
  const byCat = await api('GET', `/api/catalog/products?categoryId=${catId}`, undefined, G);
  check('consulta de produtos por categoria', byCat.data.length >= 1 && byCat.data.every((p) => p.category && p.category.id === catId));
  const upd = await api('PUT', `/api/catalog/products/${P}`, { ...prodBody, salePrice: 10.00, name: `Arroz ${run} 5kg` }, G);
  check('edição de produto', upd.status < 300 && upd.data.name.endsWith('5kg'));
  const cs = await api('GET', `/api/customers/search?search=${encodeURIComponent('Maria ' + run)}`, undefined, O);
  check('operador busca cliente ativo (resumo)', cs.status === 200 && Array.isArray(cs.data) && cs.data.some((c) => c.id === cust.data.id) && !('email' in (cs.data[0] || {})), JSON.stringify(cs.data && cs.data[0]));
  const quick = await api('POST', '/api/customers/quick', { name: `Rapido ${run}`, phone: '11988887777', document: null, contactConsent: false }, O);
  check('cadastro rápido de cliente no caixa', quick.status < 300 && quick.data && quick.data.id, 'status ' + quick.status);

  section('UC03 Estoque');
  const bal = async (id) => { const r = await api('GET', `/api/inventory/balances?search=${encodeURIComponent('ARROZ-' + run)}`, undefined, G); const b = (r.data || []).find((x) => x.product.id === id); return b ? num(b.quantity) : null; };
  check('produto novo começa sem saldo', (await bal(P)) === null || (await bal(P)) === 0);
  const inEntry = await api('POST', '/api/inventory/entries', { productId: P, quantity: 10, supplierId: sup.data.id, documentNumber: 'NF-1', note: 'Entrada inicial' }, E);
  check('estoquista registra entrada com fornecedor', inEntry.status < 300, 'status ' + inEntry.status + ' ' + JSON.stringify(inEntry.error));
  check('entrada aumenta o saldo para 10', (await bal(P)) === 10, String(await bal(P)));
  const noReason = await api('POST', '/api/inventory/adjustments', { productId: P, newQuantity: 8, reason: '', note: null }, G);
  check('ajuste sem justificativa é recusado (400)', noReason.status === 400, 'status ' + noReason.status);
  const adj = await api('POST', '/api/inventory/adjustments', { productId: P, newQuantity: 8, reason: 'Conferência física', note: null }, G);
  check('ajuste com justificativa leva o saldo a 8', adj.status < 300 && (await bal(P)) === 8, String(await bal(P)));
  const inactiveEntry = PO ? await api('POST', '/api/inventory/entries', { productId: PO, quantity: 1 }, G) : { status: 0 };
  check('entrada em produto inativo é recusada', inactiveEntry.status >= 400 && inactiveEntry.status < 500, 'status ' + inactiveEntry.status);
  const doc = await api('POST', '/api/inventory/entry-documents', { supplierId: sup.data.id, supplierName: null, documentNumber: 'NF-2', note: 'multi', items: [{ productId: P, quantity: 2 }] }, G);
  check('nota de entrada com itens', doc.status < 300, 'status ' + doc.status + ' ' + JSON.stringify(doc.error));
  check('saldo após a nota = 10', (await bal(P)) === 10, String(await bal(P)));
  const mov = await api('GET', `/api/inventory/movements?productId=${P}`, undefined, G);
  check('movimentações imutáveis registradas (ENTRY e ADJUSTMENT)', mov.data.some((m) => m.type === 'ENTRY') && mov.data.some((m) => m.type === 'ADJUSTMENT'));

  section('UC04 Venda online');
  const price = 10.00;
  const sale = await api('POST', '/api/sales', { customerId: cust.data.id, items: [{ productId: P, quantity: 2 }], payments: [{ method: 'CASH', amount: 15 }, { method: 'PIX', amount: 5 }], clientSaleId: `pdv-${run}-1` }, O);
  check('operador finaliza venda com 2 pagamentos', sale.status < 300 && sale.data && sale.data.status === 'CONFIRMED', 'status ' + sale.status + ' ' + JSON.stringify(sale.error));
  check('total usa o preço do servidor (R$ 20,00)', num(sale.data.totalAmount) === 2 * price, String(sale.data.totalAmount));
  check('estoque baixa para 8', (await bal(P)) === 8, String(await bal(P)));
  const again = await api('POST', '/api/sales', { customerId: cust.data.id, items: [{ productId: P, quantity: 2 }], payments: [{ method: 'CASH', amount: 20 }], clientSaleId: `pdv-${run}-1` }, O);
  check('mesmo clientSaleId é idempotente (mesma venda, sem nova baixa)', again.data && again.data.id === sale.data.id && (await bal(P)) === 8, 'saldo ' + (await bal(P)));
  const mismatch = await api('POST', '/api/sales', { items: [{ productId: P, quantity: 1 }], payments: [{ method: 'CASH', amount: 5 }] }, O);
  check('pagamento diferente do total é recusado', mismatch.status === 400, 'status ' + mismatch.status);
  const over = await api('POST', '/api/sales', { items: [{ productId: P, quantity: 1000 }], payments: [{ method: 'CASH', amount: 10000 }] }, O);
  check('estoque insuficiente é recusado (sem saldo negativo)', over.status === 400 && (await bal(P)) === 8, 'status ' + over.status);
  const inactiveSale = PO ? await api('POST', '/api/sales', { items: [{ productId: PO, quantity: 1 }], payments: [{ method: 'CASH', amount: 10 }] }, O) : { status: 0 };
  check('produto inativo não é vendido', inactiveSale.status >= 400 && inactiveSale.status < 500, 'status ' + inactiveSale.status);
  const disc = await api('POST', '/api/sales', { items: [{ productId: P, quantity: 1 }], payments: [{ method: 'CASH', amount: 8 }], discountAmount: 2 }, O);
  check('operador sem permissão de desconto é recusado', disc.status === 400, 'status ' + disc.status);
  const pol = await api('GET', '/api/sales/discount-policy', undefined, O);
  check('política de desconto do operador (0%)', pol.data && num(pol.data.maxDiscountPercent) === 0, JSON.stringify(pol.data));
  const gSale = await api('POST', '/api/sales', { items: [{ productId: P, quantity: 1 }], payments: [{ method: 'CASH', amount: 8 }], discountAmount: 2, clientSaleId: `pdv-${run}-g` }, G);
  check('gerente aplica desconto (total R$ 8,00)', gSale.status < 300 && num(gSale.data.totalAmount) === 8, 'status ' + gSale.status + ' ' + JSON.stringify(gSale.error));
  const fiado = await api('POST', '/api/sales', { customerId: cust.data.id, items: [{ productId: P, quantity: 1 }], payments: [{ method: 'STORE_CREDIT', amount: 10 }], clientSaleId: `pdv-${run}-f` }, O);
  check('venda em fiado (STORE_CREDIT)', fiado.status < 300, 'status ' + fiado.status + ' ' + JSON.stringify(fiado.error));
  check('"minhas vendas" do operador', (await api('GET', '/api/sales/mine', undefined, O)).data.items.some((s) => s.id === sale.data.id));

  section('Cancelamento');
  const before = await bal(P);
  check('operador não cancela venda (403)', (await api('POST', `/api/sales/${fiado.data.id}/cancel`, { reason: 'x' }, O)).status === 403);
  const noWhy = await api('POST', `/api/sales/${fiado.data.id}/cancel`, { reason: '' }, G);
  check('cancelamento exige motivo', noWhy.status === 400, 'status ' + noWhy.status);
  const cancel = await api('POST', `/api/sales/${fiado.data.id}/cancel`, { reason: 'Cliente desistiu' }, G);
  check('gerente cancela e devolve o estoque', cancel.status < 300 && (await bal(P)) === before + 1, `antes ${before} depois ${await bal(P)}`);
  const mv2 = await api('GET', `/api/inventory/movements?productId=${P}&type=SALE_CANCELLATION`, undefined, G);
  check('movimentação SALE_CANCELLATION registrada', mv2.data.length >= 1);

  section('UC05/UC06 Offline e sincronização');
  const localOk = `local-${run}-ok`;
  const off = (id, unitPrice, extra = {}) => ({ localSaleId: id, createdAt: new Date().toISOString(), customerId: null, items: [{ productId: P, quantity: 1, unitPrice }], payments: [{ method: 'CASH', amount: unitPrice }], ...extra });
  const before2 = await bal(P);
  const s_ok = await api('POST', '/api/offline/sales/sync', off(localOk, price), O);
  check('venda offline sem divergência é aceita (SENT)', s_ok.status < 300 && s_ok.data && s_ok.data.status === 'SENT', JSON.stringify(s_ok.data && s_ok.data.status) + ' ' + JSON.stringify(s_ok.error));
  check('sincronização baixa o estoque', (await bal(P)) === before2 - 1, String(await bal(P)));
  const s_dup = await api('POST', '/api/offline/sales/sync', off(localOk, price), O);
  check('reenvio da mesma venda não duplica', s_dup.data && s_dup.data.status === 'SENT' && (await bal(P)) === before2 - 1, 'saldo ' + (await bal(P)));
  const localConf = `local-${run}-conf`;
  const s_conf = await api('POST', '/api/offline/sales/sync', off(localConf, 9), O);
  check('preço diferente gera CONFLICT (PRICE_CHANGED)', s_conf.data && s_conf.data.status === 'CONFLICT' && JSON.stringify(s_conf.data).includes('PRICE_CHANGED'), JSON.stringify(s_conf.data).slice(0, 200));
  const pend = await api('GET', '/api/offline/sales/conflicts', undefined, G);
  const myConf = (pend.data || []).find((c) => c.localSaleId === localConf);
  check('conflito aparece como pendente para o gerente', !!myConf);
  check('venda original foi preservada no conflito', myConf && String(myConf.salePayload).includes(localConf));
  const rejNoNote = await api('POST', `/api/offline/sales/conflicts/${myConf.id}/resolve`, { action: 'REJECT', note: null }, G);
  check('recusar sem motivo é recusado (400)', rejNoNote.status === 400, 'status ' + rejNoNote.status);
  const stBefore = await api('GET', `/api/offline/sales/status?localSaleIds=${localConf}`, undefined, O);
  const stockBeforeAccept = await bal(P);
  const acc = await api('POST', `/api/offline/sales/conflicts/${myConf.id}/resolve`, { action: 'ACCEPT', note: 'Preço praticado validado' }, G);
  check('gerente aceita o conflito', acc.status < 300, 'status ' + acc.status + ' ' + JSON.stringify(acc.error));
  check('aceite baixa o estoque', (await bal(P)) === stockBeforeAccept - 1, String(await bal(P)));
  const stAfter = await api('GET', `/api/offline/sales/status?localSaleIds=${localConf}`, undefined, O);
  check('PDV consegue ver a decisão (status SENT)', stAfter.data && stAfter.data[0] && stAfter.data[0].status === 'SENT', JSON.stringify(stBefore.data) + ' -> ' + JSON.stringify(stAfter.data));
  const localRej = `local-${run}-rej`;
  const s_rej = await api('POST', '/api/offline/sales/sync', off(localRej, 8), O);
  const pend2 = await api('GET', '/api/offline/sales/conflicts', undefined, G);
  const rc = (pend2.data || []).find((c) => c.localSaleId === localRej);
  const rej = rc && await api('POST', `/api/offline/sales/conflicts/${rc.id}/resolve`, { action: 'REJECT', note: 'Preço incorreto' }, G);
  const stockAfterRej = await bal(P);
  check('recusa com motivo encerra sem baixar estoque', rej && rej.status < 300 && stockAfterRej === stockBeforeAccept - 1, 'status ' + (rej && rej.status) + ' saldo ' + stockAfterRej);
  const stRej = await api('GET', `/api/offline/sales/status?localSaleIds=${localRej}`, undefined, O);
  check('PDV vê a venda recusada (REJECTED)', stRej.data && stRej.data[0] && stRej.data[0].status === 'REJECTED', JSON.stringify(stRej.data));

  section('UC07 Cliente e histórico');
  const hist = await api('GET', `/api/sales?customerId=${cust.data.id}`, undefined, G);
  check('histórico do cliente lista só as vendas dele', hist.data.items.length >= 1 && hist.data.items.every((s) => s.customer && s.customer.id === cust.data.id), String(hist.data.items.length));
  const inact = await api('PUT', `/api/customers/${quick.data.id}`, { name: `Rapido ${run}`, phone: '11988887777', email: null, document: null, contactConsent: false, active: false }, G);
  const cs2 = await api('GET', `/api/customers/search?search=${encodeURIComponent('Rapido ' + run)}`, undefined, O);
  check('cliente inativo deixa de ser sugerido', inact.status < 300 && Array.isArray(cs2.data) && !cs2.data.some((c) => c.id === quick.data.id), JSON.stringify(cs2.data));

  section('UC08 Relatórios');
  const from = new Date(Date.now() - 3600e3).toISOString(), to = new Date(Date.now() + 3600e3).toISOString();
  const rep = await api('GET', `/api/sales?from=${from}&to=${to}`, undefined, G);
  check('vendas por período com totais', rep.status === 200 && rep.data.totals && rep.data.totals.saleCount >= 3, JSON.stringify(rep.data && rep.data.totals));
  const byOp = await api('GET', `/api/sales?operatorUserId=${oU.data.id}`, undefined, G);
  check('vendas por operador', byOp.data.items.length >= 1 && byOp.data.items.every((s) => s.operatorUserId === oU.data.id));
  const top = await api('GET', `/api/sales/top-products?from=${from}&to=${to}`, undefined, G);
  check('produtos mais vendidos', top.status === 200 && top.data.some((t) => t.productId === P), JSON.stringify(top.data && top.data[0]));
  const csv = await api('GET', `/api/sales/export.csv?from=${from}&to=${to}`, undefined, G);
  check('exportação CSV', csv.status === 200 && csv.text.split('\n').length >= 3, 'status ' + csv.status);
  const mvp = await api('GET', `/api/inventory/movements?from=${from}&to=${to}`, undefined, G);
  check('movimentações por período', mvp.status === 200 && mvp.data.length >= 5);

  section('Auditoria e senha');
  const aud = await api('GET', '/api/audit/events?size=50', undefined, A);
  check('auditoria registra ações críticas', aud.status === 200 && aud.data.totalElements >= 5, String(aud.data && aud.data.totalElements));
  const reset = await api('POST', `/api/access/users/${eU.data.id}/password`, { password: 'nova-senha1' }, A);
  check('admin redefine senha de outro usuário', reset.status < 300, 'status ' + reset.status);
  check('token antigo do usuário deixa de valer', (await api('GET', '/api/auth/me', undefined, E)).status === 401);
  check('login com a nova senha funciona', (await login(`estoquista-${run}`, 'nova-senha1')).status === 200);
  const selfChange = await api('POST', '/api/auth/password', { currentPassword: 'senha123', newPassword: 'outra-senha1' }, O);
  check('usuário troca a própria senha', selfChange.status < 300, 'status ' + selfChange.status);
  const deact = await api('PUT', `/api/access/users/${gU.data.id}`, { nome: 'Gerente Teste', login: `gerente-${run}`, perfil: 'GERENTE', active: false }, A);
  check('desativar usuário invalida a sessão aberta', deact.status < 300 && (await api('GET', '/api/auth/me', undefined, G)).status === 401);

  console.log(`\n==== ${pass} passaram, ${fail} falharam ====`);
  if (fail) { console.log('\nFalhas:\n- ' + failures.join('\n- ')); process.exit(1); }
})().catch((e) => { console.error('ERRO NO ROTEIRO:', e); process.exit(2); });
