import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { ThemeService } from './core/theme.service';

@Component({
  selector: 'mo-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
})
export class AppComponent {
  // Aplica o tema salvo já na tela de login, antes do menu existir.
  private readonly theme = inject(ThemeService);
}
