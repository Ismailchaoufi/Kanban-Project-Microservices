import {Component, OnInit} from '@angular/core';
import {KanbanBoardComponent} from '../../tasks/kanban-board/kanban-board.component';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatChipsModule} from '@angular/material/chips';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatDialogModule, MatDialog} from '@angular/material/dialog';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatMenuModule} from '@angular/material/menu';
import {FormsModule} from '@angular/forms';
import {Project, Member} from '../../../core/models/project.model';
import {User} from '../../../core/models/user.model';
import {ActivatedRoute} from '@angular/router';
import {ProjectService} from '../../../core/services/project.service';
import {AuthService} from '../../../core/services/auth.service';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    FormsModule,
    KanbanBoardComponent
  ],
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.css'
})
export class ProjectDetailComponent implements OnInit {
  projectId!: number;
  project?: Project;
  loading = true;
  members: Member[] = [];
  allUsers: User[] = [];
  showMembersPanel = false;
  showAddMemberModal = false;
  selectedUserId: number | null = null;
  loadingMembers = false;
  loadingUsers = false;
  currentUserId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.currentUserId = this.authService.getCurrentUser()?.id || null;
    this.loadProject();
    this.loadMembers();
  }

  loadProject(): void {
    this.projectService.getProjectById(this.projectId).subscribe({
      next: (project) => {
        this.project = project;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadMembers(): void {
    this.loadingMembers = true;
    this.projectService.getMembers(this.projectId).subscribe({
      next: (members) => {
        this.members = members;
        this.loadingMembers = false;
      },
      error: () => {
        this.loadingMembers = false;
        this.showSnackBar('Failed to load members', 'error');
      }
    });
  }

  loadAllUsers(): void {
    this.loadingUsers = true;
    this.authService.getAllUsers().subscribe({
      next: (response) => {
        // Filter out users who are already members
        const memberUserIds = new Set(this.members.map(m => m.userId));
        this.allUsers = response.content.filter(user => !memberUserIds.has(user.id));
        this.loadingUsers = false;
      },
      error: () => {
        this.loadingUsers = false;
        this.showSnackBar('Failed to load users', 'error');
      }
    });
  }

  toggleMembersPanel(): void {
    this.showMembersPanel = !this.showMembersPanel;
  }

  openAddMemberModal(): void {
    this.loadAllUsers();
    this.showAddMemberModal = true;
    this.selectedUserId = null;
  }

  closeAddMemberModal(): void {
    this.showAddMemberModal = false;
    this.selectedUserId = null;
  }

  addMember(): void {
    if (!this.selectedUserId) {
      this.showSnackBar('Please select a user', 'error');
      return;
    }

    this.projectService.addMember(this.projectId, this.selectedUserId).subscribe({
      next: (member) => {
        this.members.push(member);
        this.closeAddMemberModal();
        this.showSnackBar('Member added successfully', 'success');
      },
      error: (error) => {
        this.showSnackBar(error.error?.message || 'Failed to add member', 'error');
      }
    });
  }

  removeMember(userId: number, memberName: string): void {
    if (confirm(`Are you sure you want to remove ${memberName} from this project?`)) {
      this.projectService.removeMember(this.projectId, userId).subscribe({
        next: () => {
          this.members = this.members.filter(m => m.userId !== userId);
          this.showSnackBar('Member removed successfully', 'success');
        },
        error: (error) => {
          this.showSnackBar(error.error?.message || 'Failed to remove member', 'error');
        }
      });
    }
  }

  isOwner(): boolean {
    return this.project?.ownerId === this.currentUserId;
  }

  getMemberInitials(member: Member): string {
    return `${member.firstName.charAt(0)}${member.lastName.charAt(0)}`.toUpperCase();
  }

  getUserInitials(user: User): string {
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  }

  private showSnackBar(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: type === 'success' ? 'snackbar-success' : 'snackbar-error',
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }
}
