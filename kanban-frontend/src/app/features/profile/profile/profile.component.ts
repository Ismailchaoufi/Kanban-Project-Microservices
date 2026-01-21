import {Component, OnInit} from '@angular/core';
import {User} from '../../../core/models/user.model';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {AuthService} from '../../../core/services/auth.service';
import {MatIconModule} from '@angular/material/icon';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  currentUser: User | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
  }
}
