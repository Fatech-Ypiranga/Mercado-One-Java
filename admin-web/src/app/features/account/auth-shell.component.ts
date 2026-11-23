import { Component, ViewEncapsulation } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Moldura das telas de acesso (entrar, esqueci a senha, suporte): fundo, marca e o card de cada tela
 * entra pela saída de rota. Sem encapsulamento de estilo, porque as três telas compartilham as classes
 * "login-*" deste arquivo CSS, que só é carregado quando uma delas é aberta.
 */
@Component({
  selector: 'mo-auth-shell',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './auth-shell.component.html',
  styleUrl: './auth-shell.component.css',
  encapsulation: ViewEncapsulation.None,
})
export class AuthShellComponent {
  /** Linhas da tabela desfocada do fundo (decorativas). */
  protected readonly rows = [1, 2, 3, 4, 5, 6, 7];
}
