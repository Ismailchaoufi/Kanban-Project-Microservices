import { Component, OnInit, OnDestroy } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { Observable, filter, Subject, takeUntil } from 'rxjs';

import { SidebarComponent } from './shared/sidebar/sidebar.component';
import { NavbarComponent } from './shared/navbar/navbar.component';
import { AuthService } from './core/services/auth.service';
import { User } from './core/models/user.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatSidenavModule,
    NavbarComponent,
    SidebarComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Kanban Board';
  isAuthenticated = false;
  currentUser$!: Observable<User | null>;
  sidenavOpened = true;
  isPublicPage = false;

  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    // Écouter les changements de route
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        this.checkIfPublicPage(event.url);
      });
  }

  ngOnInit(): void {
    // Vérifier la route actuelle IMMÉDIATEMENT
    this.checkIfPublicPage(this.router.url);

    this.isAuthenticated = this.authService.isAuthenticated();
    this.currentUser$ = this.authService.currentUser$;

    // ✅ NE PAS rediriger automatiquement ici
    // Le auth.guard s'en charge
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.isAuthenticated = user !== null;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private checkIfPublicPage(url: string): void {
    const publicRoutes = [
      '/auth/login',
      '/auth/register',
      '/invitations/accept'
    ];

    this.isPublicPage = publicRoutes.some(route => url.startsWith(route));
  }

  toggleSidenav(): void {
    this.sidenavOpened = !this.sidenavOpened;
  }

  get shouldShowNavigation(): boolean {
    return this.isAuthenticated && !this.isPublicPage;
  }
}
