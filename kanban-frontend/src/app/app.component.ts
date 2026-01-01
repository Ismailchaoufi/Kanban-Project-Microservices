import {Component, OnInit} from '@angular/core';
import {Router, RouterOutlet} from '@angular/router';
import {SidebarComponent} from './shared/sidebar/sidebar.component';
import {NavbarComponent} from './shared/navbar/navbar.component';
import {MatSidenavModule} from '@angular/material/sidenav';
import {CommonModule} from '@angular/common';
import {AuthService} from './core/services/auth.service';
import {User} from './core/models/user.model';
import {Observable} from 'rxjs';

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
export class AppComponent implements OnInit{
  title = 'Kanban Board';
  isAuthenticated = false;
  currentUser$!: Observable<User | null>;
  sidenavOpened = true;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.currentUser$ = this.authService.currentUser$;

    this.authService.currentUser$.subscribe(user => {
      this.isAuthenticated = user !== null;
      if (!user && !this.router.url.includes('/auth')) {
        this.router.navigate(['/auth/login']);
      }
    });
  }

  toggleSidenav(): void {
    this.sidenavOpened = !this.sidenavOpened;
  }
}
