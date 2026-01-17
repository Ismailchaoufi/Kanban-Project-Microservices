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

  // Détecter si on est sur une page publique
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
    this.isAuthenticated = this.authService.isAuthenticated();
    this.currentUser$ = this.authService.currentUser$;

    // Vérifier la route actuelle au chargement
    this.checkIfPublicPage(this.router.url);

    this.authService.currentUser$.subscribe(user => {
      this.isAuthenticated = user !== null;

      // Ne rediriger que si pas sur page publique
      if (!user && !this.isPublicPage) {
        this.router.navigate(['/auth/login']);
      }
    });
  }

  /**
   * vérifier si la route actuelle est publique
   */
  private checkIfPublicPage(url: string): void {
    const publicRoutes = [
      '/auth/login',
      '/auth/register',
      '/invitations/accept'
    ];

    this.isPublicPage = publicRoutes.some(route => url.startsWith(route));
  }

  /**
   * Toggle sidebar
   */
  toggleSidenav(): void {
    this.sidenavOpened = !this.sidenavOpened;
  }

  /**
   * Helper : Afficher navbar/sidebar ?
   */
  get shouldShowNavigation(): boolean {
    return this.isAuthenticated && !this.isPublicPage;
  }
}
