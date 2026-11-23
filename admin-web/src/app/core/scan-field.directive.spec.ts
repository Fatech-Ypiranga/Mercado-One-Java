import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';

import { ScanFieldDirective } from './scan-field.directive';

@Component({
  standalone: true,
  imports: [FormsModule, ScanFieldDirective],
  template: `
    <form (ngSubmit)="submitted = submitted + 1">
      <input id="name" name="name" [(ngModel)]="name" />
      <input id="barcode" name="barcode" [(ngModel)]="barcode" moScanField />
      <input id="hidden" type="hidden" name="h" />
      <input id="off" name="off" disabled />
      <input id="sku" name="sku" [(ngModel)]="sku" />
      <button type="submit">Salvar</button>
    </form>
  `,
})
class HostComponent {
  name = '';
  barcode = '';
  sku = '';
  submitted = 0;
}

@Component({
  standalone: true,
  imports: [ScanFieldDirective],
  template: `<form><input id="solo" name="solo" moScanField /></form>`,
})
class LastFieldHostComponent {}

describe('ScanFieldDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  const field = (id: string) => fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;

  /** O leitor termina a leitura com Enter. */
  const pressEnter = (id: string) => {
    const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
    field(id).dispatchEvent(event);
    return event;
  };

  beforeEach(() => {
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
  });

  afterEach(() => fixture.nativeElement.remove());

  it('does not submit the form when the scanner sends Enter', async () => {
    field('barcode').focus();
    const event = pressEnter('barcode');
    fixture.detectChanges();

    expect(event.defaultPrevented).toBeTrue();
    expect(host.submitted).toBe(0);
  });

  it('moves focus to the next field, skipping hidden and disabled ones', () => {
    field('barcode').focus();
    pressEnter('barcode');

    expect(document.activeElement).toBe(field('sku'));
  });

  it('keeps submitting the form on Enter in fields without the directive', () => {
    // Comportamento nativo preservado fora do campo do leitor: o Enter dos outros campos não é bloqueado.
    const event = pressEnter('name');
    expect(event.defaultPrevented).toBeFalse();
  });

  it('does nothing harmful when it is the last field of the form', () => {
    const lone = TestBed.createComponent(LastFieldHostComponent);
    document.body.appendChild(lone.nativeElement);
    lone.detectChanges();
    const solo = lone.nativeElement.querySelector('#solo') as HTMLInputElement;
    const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });

    expect(() => solo.dispatchEvent(event)).not.toThrow();
    expect(event.defaultPrevented).toBeTrue();
    lone.nativeElement.remove();
  });
});
