import { AfterViewInit, Component, ElementRef, inject, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formatWhatsapp, SUPPORT_CONTACTS, whatsappLink } from './support-contacts';

/** "Fale com o suporte". Mostra só os canais configurados no ambiente; não há contato de exemplo na tela. */
@Component({
  selector: 'mo-support-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './support-page.component.html',
})
export class SupportPageComponent implements AfterViewInit {
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');
  private readonly contacts = inject(SUPPORT_CONTACTS);

  protected readonly whatsappHref = whatsappLink(this.contacts.whatsapp);
  protected readonly whatsappText = formatWhatsapp(this.contacts.whatsapp);
  protected readonly email = this.contacts.email.trim();
  protected readonly hours = this.contacts.hours.trim();
  protected readonly hasChannel = this.whatsappHref !== null || this.email !== '';

  ngAfterViewInit(): void {
    this.heading().nativeElement.focus({ preventScroll: true });
  }
}
