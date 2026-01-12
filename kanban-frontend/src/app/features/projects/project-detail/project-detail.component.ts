import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Subject, takeUntil, finalize, forkJoin, catchError, of } from 'rxjs';
import { FormsModule } from '@angular/forms';

// Angular Material Imports
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

// Project Imports
import { KanbanBoardComponent } from '../../tasks/kanban-board/kanban-board.component';
import { Project, Member } from '../../../core/models/project.model';
import { User } from '../../../core/models/user.model';
import { ProjectService } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import {MatInputModule} from '@angular/material/input';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    KanbanBoardComponent
  ],
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.css'
})
export class ProjectDetailComponent implements OnInit, OnDestroy {
  // Core data properties
  projectId!: number;
  project?: Project;
  members: Member[] = [];
  currentUserId: number | null = null;
  inviteEmail: string = '';
  inviting = false;

  // UI state flags
  loading = true;
  loadingMembers = false;
  showMembersPanel = false;
  showAddMemberModal = false;

  // Cleanup subject for subscriptions
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly projectService: ProjectService,
    private readonly authService: AuthService,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }


  inviteMember(): void {
    if (!this.inviteEmail || !this.isValidEmail(this.inviteEmail)) {
      this.showSnackBar('Please enter a valid email address', 'error');
      return;
    }

    // Check if email already exists in current members
    const emailExists = this.members.some(
      member => member.email.toLowerCase() === this.inviteEmail.toLowerCase()
    );

    if (emailExists) {
      this.showSnackBar('This user is already a member of the project', 'error');
      return;
    }

    this.inviting = true;

    this.projectService.inviteMember(this.projectId, this.inviteEmail)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.inviting = false)
      )
      .subscribe({
        next: (response) => {
          if (response.userExists) {
            this.showSnackBar('User added successfully to the project', 'success');
            this.loadMembers();
          } else {
            this.showSnackBar('Invitation email sent successfully', 'success');
          }
          this.closeAddMemberModal();
        },
        error: (error) => {
          const message = error.error?.message || 'Failed to send invitation';
          this.showSnackBar(message, 'error'); // <-- show error in UI
        }
      });
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }



  /**
   * Initialize component by loading project ID and data
   */
  private initializeComponent(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.currentUserId = this.authService.getCurrentUser()?.id || null;

    if (!this.projectId) {
      this.handleError('Invalid project ID');
      this.loading = false;
      return;
    }

    this.loadInitialData();
  }

  /**
   * Load project and members data in parallel for better performance
   */
  private loadInitialData(): void {
    this.loading = true;

    forkJoin({
      project: this.projectService.getProjectById(this.projectId).pipe(
        catchError(error => {
          console.error('Error loading project:', error);
          return of(null);
        })
      ),
      members: this.projectService.getMembers(this.projectId).pipe(
        catchError(error => {
          console.error('Error loading members:', error);
          return of([]);
        })
      )
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: ({ project, members }) => {
          if (project) {
            this.project = project;
            this.members = members;
          } else {
            this.handleError('Project not found');
          }
        },
        error: (error) => this.handleError('Failed to load project data', error)
      });
  }

  /**
   * Reload members list
   */
  loadMembers(): void {
    this.loadingMembers = true;

    this.projectService.getMembers(this.projectId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingMembers = false)
      )
      .subscribe({
        next: (members) => {
          this.members = members;
        },
        error: (error) => {
          this.handleError('Failed to load members', error);
        }
      });
  }


  /**
   * Toggle members panel visibility
   */
  toggleMembersPanel(): void {
    this.showMembersPanel = !this.showMembersPanel;

    // Close add member modal if open
    if (this.showAddMemberModal) {
      this.closeAddMemberModal();
    }
  }

  /**
   * Open the add member modal
   */
  openAddMemberModal(): void {
    this.showAddMemberModal = true;
    this.inviteEmail = '';
  }

  /**
   * Close the add member modal
   */
  closeAddMemberModal(): void {
    this.showAddMemberModal = false;
    this.inviteEmail = '';
  }

  /**
   * Remove a member from the project
   */
  removeMember(userId: number, memberName: string): void {
    if (!this.confirmAction(`Are you sure you want to remove ${memberName} from this project?`)) {
      return;
    }

    this.projectService.removeMember(this.projectId, userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.members = this.members.filter(m => m.userId !== userId);
          this.showSnackBar(`${memberName} removed successfully`, 'success');
        },
        error: (error) => {
          const message = error.error?.message || 'Failed to remove member';
          this.handleError(message, error);
        }
      });
  }

  /**
   * Check if current user is the project owner
   */
  isOwner(): boolean {
    return this.project?.ownerId === this.currentUserId;
  }

  isAdmin(): boolean {
    const user = this.authService.getCurrentUser();
    return user?.role === "ADMIN"; // adjust this according to your role logic
  }

  /**
   * Get member initials for avatar display
   */
  getMemberInitials(member: Member): string {
    return this.getInitials(member.firstName, member.lastName);
  }

  /**
   * Get user initials for avatar display
   */
  getUserInitials(user: User): string {
    return this.getInitials(user.firstName, user.lastName);
  }

  /**
   * Generate initials from first and last name
   */
  private getInitials(firstName: string, lastName: string): string {
    const first = firstName?.charAt(0)?.toUpperCase() || '';
    const last = lastName?.charAt(0)?.toUpperCase() || '';
    return `${first}${last}`;
  }

  /**
   * Generate consistent avatar color based on user ID
   */
  getAvatarColor(userId: number): string {
    const colors = [
      '#1976d2', '#388e3c', '#d32f2f', '#7b1fa2',
      '#f57c00', '#0097a7', '#c2185b', '#5d4037',
      '#455a64', '#e64a19'
    ];
    return colors[userId % colors.length];
  }

  /**
   * TrackBy function for ngFor optimization
   */
  trackByUserId(index: number, member: Member): number {
    return member.userId;
  }

  /**
   * TrackBy function for users list
   */
  trackByUserIdFn(index: number, user: User): number {
    return user.id;
  }

  /**
   * Show confirmation dialog
   */
  private confirmAction(message: string): boolean {
    return confirm(message);
  }

  /**
   * Display snackbar message
   */
  private showSnackBar(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Close', {
      duration: type === 'success' ? 3000 : 5000,
      panelClass: type === 'success' ? 'snackbar-success' : 'snackbar-error',
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }

  /**
   * Handle errors with logging and user notification
   */
  private handleError(message: string, error?: any): void {
    if (error) {
      console.error(message, error);
    }
    this.showSnackBar(message, 'error');
  }


}
