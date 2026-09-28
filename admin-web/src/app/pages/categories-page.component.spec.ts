import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { CatalogService, Category } from '../core/catalog.service';
import { CategoriesPageComponent } from './categories-page.component';

describe('CategoriesPageComponent', () => {
  const now = new Date().toISOString();
  const category: Category = { id: 1, name: 'Bebidas', active: true, createdAt: now, updatedAt: now };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoriesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
  });

  it('renders loaded categories', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [category], error: null, timestamp: now }));

    const fixture = TestBed.createComponent(CategoriesPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Bebidas');
    expect(fixture.nativeElement.textContent).toContain('Ativa');
  });

  it('creates categories and reloads the list', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const create = spyOn(catalog, 'createCategory').and.returnValue(of({ success: true, data: category, error: null, timestamp: now }));
    const fixture = TestBed.createComponent(CategoriesPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].setValue({ name: 'Bebidas', active: true });
    fixture.nativeElement.querySelector('form.record-sheet').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(create).toHaveBeenCalledWith({ name: 'Bebidas', active: true });
    expect(fixture.nativeElement.textContent).toContain('Categoria salva.');
  });

  it('updates categories without creating a new one', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [category], error: null, timestamp: now }));
    const update = spyOn(catalog, 'updateCategory').and.returnValue(of({ success: true, data: category, error: null, timestamp: now }));
    const fixture = TestBed.createComponent(CategoriesPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['edit'](category);
    fixture.componentInstance['form'].setValue({ name: 'Bebidas Frias', active: false });
    fixture.nativeElement.querySelector('form.record-sheet').dispatchEvent(new Event('submit'));

    expect(update).toHaveBeenCalledWith(1, { name: 'Bebidas Frias', active: false });
  });

  it('shows feedback when saving fails', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    spyOn(catalog, 'createCategory').and.returnValue(throwError(() => new Error('fail')));
    const fixture = TestBed.createComponent(CategoriesPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].setValue({ name: 'Bebidas', active: true });
    fixture.nativeElement.querySelector('form.record-sheet').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Revise os dados da categoria.');
    expect(fixture.componentInstance['form'].value.name).toBe('Bebidas');
  });

  it('ignores an older filter response and keeps the latest list', () => {
    const catalog = TestBed.inject(CatalogService);
    const first = new Subject<any>();
    const second = new Subject<any>();
    const list = spyOn(catalog, 'listCategories').and.returnValues(first.asObservable(), second.asObservable());
    const fixture = TestBed.createComponent(CategoriesPageComponent);
    fixture.detectChanges();
    fixture.componentInstance['searchControl'].setValue('Bebidas');
    fixture.componentInstance['load']();
    expect(list).toHaveBeenCalledTimes(2);
    second.next({ success: true, data: [category], error: null, timestamp: now });
    first.next({ success: true, data: [], error: null, timestamp: now });
    expect(fixture.nativeElement.textContent).toContain('Bebidas');
  });

  it('shows the form step after selecting a category', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [category], error: null, timestamp: now }));
    const fixture = TestBed.createComponent(CategoriesPageComponent);
    fixture.detectChanges();
    fixture.componentInstance['edit'](category);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.workspace').classList.contains('mobile-form-step')).toBeTrue();
  });

  it('prevents repeated submissions while a save is pending', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const pending = new Subject<any>();
    const create = spyOn(catalog, 'createCategory').and.returnValue(pending.asObservable());
    const fixture = TestBed.createComponent(CategoriesPageComponent);
    fixture.detectChanges();
    fixture.componentInstance['form'].setValue({ name: 'Bebidas', active: true });
    fixture.componentInstance['save']();
    fixture.componentInstance['save']();
    expect(create).toHaveBeenCalledTimes(1);
    pending.next({ success: true, data: category, error: null, timestamp: now });
    pending.complete();
  });
});
