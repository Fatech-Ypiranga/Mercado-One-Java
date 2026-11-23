import { AfterViewInit, Component, ElementRef, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * "Esqueci a senha". Neste sistema a senha é redefinida pelo administrador do mercado (Usuários > Redefinir
 * senha); não existe envio de link por e-mail. A tela explica isso em vez de simular um envio que não acontece.
 */
@Component({
  selector: 'mo-forgot-password-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './forgot-password-page.component.html',
})
export class ForgotPasswordPageComponent implements AfterViewInit {
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');

  ngAfterViewInit(): void {
    this.heading().nativeElement.focus({ preventScroll: true });
  }
}
