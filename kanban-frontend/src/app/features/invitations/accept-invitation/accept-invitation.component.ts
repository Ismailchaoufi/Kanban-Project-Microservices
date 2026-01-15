import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import {InvitationService} from '../../../core/services/invitation.service';
import {AuthService} from '../../../core/services/auth.service';


@Component({
  selector: 'app-accept-invitation',
  standalone: true,
  imports: [
    CommonModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule
  ],
  templateUrl: './accept-invitation.component.html',
  styleUrls: ['./accept-invitation.component.css']
})
export class AcceptInvitationComponent implements OnInit {
  loading = true;
  token: string | null = null;

  // États possibles
  state: 'loading' | 'not-registered' | 'registered' | 'success' | 'error' = 'loading';

  // Informations de l'invitation
  invitationEmail: string = '';
  projectName: string = '';
  invitedBy: string = '';
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invitationService: InvitationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');

    if (!this.token) {
      this.state = 'error';
      this.errorMessage = 'Invalid invitation link';
      this.loading = false;
      return;
    }

    // Vérifier l'invitation d'abord
    this.verifyInvitation();
  }

  private verifyInvitation(): void {
    if (!this.token) return;

    this.invitationService.verifyInvitation(this.token).subscribe({
      next: (info) => {
        if (!info.valid) {
          this.state = 'error';
          this.errorMessage = info.expired
            ? 'This invitation has expired'
            : 'This invitation is no longer valid';
          this.loading = false;
          return;
        }

        this.invitationEmail = info.email;
        this.projectName = info.projectName;
        this.invitedBy = info.invitedBy;

        // Vérifier si l'utilisateur est connecté
        this.checkAuthenticationStatus();
      },
      error: (error) => {
        this.state = 'error';
        this.errorMessage = error.error?.message || 'Failed to verify invitation';
        this.loading = false;
      }
    });
  }

  private checkAuthenticationStatus(): void {
    const isAuthenticated = this.authService.isAuthenticated();

    if (!isAuthenticated) {
      // Utilisateur pas connecté → Rediriger vers inscription
      this.state = 'not-registered';
      this.loading = false;
    } else {
      // Utilisateur connecté → Vérifier email
      const currentUser = this.authService.getCurrentUser();

      if (currentUser?.email.toLowerCase() === this.invitationEmail.toLowerCase()) {
        // Bon utilisateur → Accepter automatiquement
        this.acceptInvitation();
      } else {
        // Mauvais utilisateur
        this.state = 'error';
        this.errorMessage = `This invitation was sent to ${this.invitationEmail}. Please logout and sign in with that email.`;
        this.loading = false;
      }
    }
  }

  private acceptInvitation(): void {
    if (!this.token) return;

    this.loading = true;
    this.state = 'loading';

    this.invitationService.acceptInvitation(this.token).subscribe({
      next: () => {
        this.state = 'success';
        this.loading = false;

        // Rediriger vers les projets après 2 secondes
        setTimeout(() => {
          this.router.navigate(['/projects']);
        }, 2000);
      },
      error: (error) => {
        this.state = 'error';
        this.errorMessage = error.error?.message || 'Failed to accept invitation';
        this.loading = false;
      }
    });
  }

  goToRegister(): void {
    // Rediriger vers inscription avec email pré-rempli et token
    this.router.navigate(['/auth/register'], {
      queryParams: {
        email: this.invitationEmail,
        invitationToken: this.token
      }
    });
  }

  goToLogin(): void {
    // Rediriger vers login avec token pour revenir après
    this.router.navigate(['/auth/login'], {
      queryParams: {
        returnUrl: `/invitations/accept?token=${this.token}`
      }
    });
  }

  goToProjects(): void {
    this.router.navigate(['/projects']);
  }
}
