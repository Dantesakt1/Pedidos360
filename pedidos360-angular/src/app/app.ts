import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { RouterLink, RouterOutlet, Router, RouterLinkActive } from '@angular/router'; // <-- Importamos Router
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { AccountInfo, EventMessage, EventType, InteractionStatus } from '@azure/msal-browser';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { environment } from './environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  user: AccountInfo | null = null;
  private readonly destroying$ = new Subject<void>();

  constructor(
    private authService: MsalService,
    private msalBroadcastService: MsalBroadcastService,
    private cdr: ChangeDetectorRef,
    private router: Router // <-- Inyectamos el Router
  ) { }

  ngOnInit(): void {
    // 1. Escuchar el evento EXACTO de cuando el login fue exitoso
    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) => msg.eventType === EventType.LOGIN_SUCCESS),
        takeUntil(this.destroying$)
      )
      .subscribe((result: any) => {
        const payload = result.payload;
        this.authService.instance.setActiveAccount(payload.account);
        this.actualizarUsuario();
      });

    // 2. Esperar a que MSAL termine todas sus validaciones antes de hacer nada
    this.msalBroadcastService.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None),
        takeUntil(this.destroying$)
      )
      .subscribe(() => {
        this.actualizarUsuario();
      });
  }

  private actualizarUsuario(): void {
    let activeAccount = this.authService.instance.getActiveAccount();
    const accounts = this.authService.instance.getAllAccounts();

    if (!activeAccount && accounts.length > 0) {
      activeAccount = accounts[0];
      this.authService.instance.setActiveAccount(activeAccount);
    }

    this.user = activeAccount ?? null;

    // LA MAGIA: Solo redirigimos cuando MSAL ya confirmó que existe el usuario
    if (this.user && this.router.url === '/') {
      this.router.navigate(['/catalog']); // O puedes poner '/orders'
    }

    this.cdr.markForCheck();
  }

  login(): void {
    this.authService.loginRedirect({
      scopes: ['openid', 'profile', 'email']
    });
  }

  logout(): void {
    this.authService.logoutRedirect({
      postLogoutRedirectUri: 'http://localhost:4200'
    });
  }

  ngOnDestroy(): void {
    this.destroying$.next();
    this.destroying$.complete();
  }
}