import {Component, OnInit} from '@angular/core';
import {User} from '../../../core/models/user.model';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {AuthService} from '../../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule
  ],
  template: `
    <div class="profile-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>My Profile</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="profile-info" *ngIf="currentUser">
            <h2>{{ currentUser.firstName }} {{ currentUser.lastName }}</h2>
            <p>{{ currentUser.email }}</p>
            <p>Role: {{ currentUser.role }}</p>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .profile-container {
      max-width: 600px;
      margin: 0 auto;
    }
    .profile-info {
      padding: 20px;
    }
  `]
})
export class ProfileComponent implements OnInit {
  currentUser: User | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
  }
}
