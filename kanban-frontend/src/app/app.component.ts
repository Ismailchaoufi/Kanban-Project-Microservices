import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { Observable, filter } from 'rxjs';

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
export class AppComponent implements OnInit {
  title = 'Kanban Board';
  isAuthenticated = false;
  currentUser$!: Observable<User | null>;
  sidenavOpened = true;
  isPublicPage = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    // Écouter les changements de route
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.checkIfPublicPage(event.url);
      });
  }

  ngOnInit(): void {
    // ✅ Vérifier la route AVANT tout
    this.checkIfPublicPage(this.router.url);

    this.isAuthenticated = this.authService.isAuthenticated();
    this.currentUser$ = this.authService.currentUser$;

    this.authService.currentUser$.subscribe(user => {
      this.isAuthenticated = user !== null;

      // ✅ IMPORTANT : Utiliser setTimeout pour laisser le router se charger
      setTimeout(() => {
        // Re-vérifier si on est sur une page publique
        this.checkIfPublicPage(this.router.url);

        // Ne rediriger QUE si pas authentifié ET pas sur page publique
        if (!user && !this.isPublicPage) {
          this.router.navigate(['/auth/login']);
        }
      }, 0);
    });
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
