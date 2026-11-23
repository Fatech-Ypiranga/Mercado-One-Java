import { Directive, ElementRef, HostListener, inject } from '@angular/core';

/**
 * Campo que recebe a leitura de um leitor de código de barras.
 *
 * O leitor funciona como um teclado: "digita" o código e termina com Enter. Num formulário, esse Enter
 * enviaria o cadastro antes de o usuário preencher o resto. Aqui o Enter não envia: o foco passa para o
 * próximo campo do formulário.
 */
@Directive({
  selector: 'input[moScanField]',
  standalone: true,
})
export class ScanFieldDirective {
  private readonly input = inject<ElementRef<HTMLInputElement>>(ElementRef);

  @HostListener('keydown.enter', ['$event'])
  onEnter(event: Event): void {
    event.preventDefault();
    const form = this.input.nativeElement.form;
    if (!form) {
      return;
    }
    const fields = Array.from(form.elements).filter(isFocusableField);
    const next = fields[fields.indexOf(this.input.nativeElement) + 1];
    next?.focus();
  }
}

function isFocusableField(element: Element): element is HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement {
  if (element instanceof HTMLInputElement) {
    return !element.disabled && element.type !== 'hidden';
  }
  return (element instanceof HTMLSelectElement || element instanceof HTMLTextAreaElement) && !element.disabled;
}
