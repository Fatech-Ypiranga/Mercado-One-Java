import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ForgotPasswordPageComponent } from './forgot-password-page.component';

describe('ForgotPasswordPageComponent', () => {
  function render() {
    TestBed.configureTestingModule({ imports: [ForgotPasswordPageComponent], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(ForgotPasswordPageComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
    return fixture;
  }

  it('explains that the administrator resets the password, as this system has no e-mail reset', () => {
    const fixture = render();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Esqueceu a senha?');
    expect(text).toContain('Redefinir senha');
    expect(text).toContain('Minha conta');
    fixture.nativeElement.remove();
  });

  it('does not offer a form that pretends to send an e-mail', () => {
    const fixture = render();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('form')).toBeNull();
    expect(root.querySelector('input')).toBeNull();
    expect(root.textContent).not.toContain('Enviar link');
    fixture.nativeElement.remove();
  });

  it('links back to the login and on to support, and focuses the title', () => {
    const fixture = render();
    const root = fixture.nativeElement as HTMLElement;
    const links = Array.from(root.querySelectorAll('a')).map((a) => a.getAttribute('href'));

    expect(links).toContain('/login');
    expect(links).toContain('/suporte');
    expect(document.activeElement).toBe(root.querySelector('h1'));
    fixture.nativeElement.remove();
  });
});
