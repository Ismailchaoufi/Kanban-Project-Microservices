import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';
import { InvitationService } from '../../../core/services/invitation.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  loading = false;
  hidePassword = true;
  hideConfirmPassword = true;

  // ✅ Nouveau : Support invitation
  invitationToken: string | null = null;
  prefilledEmail: string | null = null;
  isFromInvitation = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private invitationService: InvitationService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // ✅ Récupérer les paramètres d'invitation
    this.route.queryParams.subscribe(params => {
      this.invitationToken = params['invitationToken'] || null;
      this.prefilledEmail = params['email'] || null;
      this.isFromInvitation = !!this.invitationToken;
    });

    this.initializeForm();
  }

  private initializeForm(): void {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: [
        { value: this.prefilledEmail || '', disabled: this.isFromInvitation },
        [Validators.required, Validators.email]
      ],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).*$/)
      ]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');

    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
    } else {
      confirmPassword?.setErrors(null);
    }

    return null;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const { confirmPassword, ...registerData } = this.registerForm.getRawValue();

    this.authService.register(registerData).subscribe({
      next: () => {
        this.showMessage('Registration successful!', 'success');

        // ✅ Si vient d'une invitation, accepter l'invitation
        if (this.invitationToken) {
          this.acceptPendingInvitation();
        } else {
          // Sinon, rediriger normalement vers dashboard
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 1500);
        }
      },
      error: (error) => {
        this.loading = false;
        const message = error.error?.message || error.message || 'Registration failed';
        this.showMessage(message, 'error');
      }
    });
  }

  /**
   * ✅ Accepter l'invitation automatiquement après inscription
   */
  private acceptPendingInvitation(): void {
    if (!this.invitationToken) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.invitationService.acceptInvitation(this.invitationToken).subscribe({
      next: () => {
        this.showMessage('You have joined the project successfully!', 'success');
        setTimeout(() => {
          this.router.navigate(['/projects']);
        }, 1500);
      },
      error: (error) => {
        console.error('Failed to accept invitation:', error);
        this.showMessage('Registered successfully, but failed to join project', 'error');
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 2000);
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  goToLogin(): void {
    // ✅ Conserver le token d'invitation si présent
    if (this.invitationToken) {
      this.router.navigate(['/auth/login'], {
        queryParams: {
          returnUrl: `/invitations/accept?token=${this.invitationToken}`
        }
      });
    } else {
      this.router.navigate(['/auth/login']);
    }
  }

  private showMessage(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Close', {
      duration: type === 'success' ? 3000 : 5000,
      panelClass: type === 'success' ? 'snackbar-success' : 'snackbar-error',
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }

  /**
   * ✅ Helper pour vérifier si formulaire est valide
   */
  get isFormValid(): boolean {
    return this.registerForm.valid;
  }

  /**
   * ✅ Getters pour faciliter l'accès aux contrôles dans le template
   */
  get firstName() {
    return this.registerForm.get('firstName');
  }

  get lastName() {
    return this.registerForm.get('lastName');
  }

  get email() {
    return this.registerForm.get('email');
  }

  get password() {
    return this.registerForm.get('password');
  }

  get confirmPassword() {
    return this.registerForm.get('confirmPassword');
  }
}
