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
import { MatInputModule } from '@angular/material/input';

// Project Imports
import { KanbanBoardComponent } from '../../tasks/kanban-board/kanban-board.component';
import { Project, Member } from '../../../core/models/project.model';
import { User } from '../../../core/models/user.model';
import { ProjectService } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';

interface LoadState {
  project: boolean;
  members: boolean;
}

type SnackBarType = 'success' | 'error' | 'info';

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
  // Core data
  projectId!: number;
  project?: Project;
  members: Member[] = [];

  // Form state
  inviteEmail = '';
  inviting = false;

  // UI state
  loading = true;
  loadingMembers = false;
  showMembersPanel = false;
  showAddMemberModal = false;

  // User context
  private currentUserId: number | null = null;
  private readonly destroy$ = new Subject<void>();

  // Avatar colors palette
  private readonly avatarColors = [
    '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e',
    '#f59e0b', '#10b981', '#06b6d4', '#3b82f6',
    '#14b8a6', '#a855f7', '#ef4444', '#84cc16'
  ] as const;

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

  // === Initialization ===

  private initializeComponent(): void {
    this.projectId = this.extractProjectId();
    this.currentUserId = this.authService.getCurrentUser()?.id ?? null;

    if (!this.validateProjectId()) {
      this.handleInitializationError();
      return;
    }

    this.loadInitialData();
  }

  private extractProjectId(): number {
    return Number(this.route.snapshot.paramMap.get('id'));
  }

  private validateProjectId(): boolean {
    return !isNaN(this.projectId) && this.projectId > 0;
  }

  private handleInitializationError(): void {
    this.loading = false;
    this.showSnackBar('Invalid project ID', 'error');
  }

  private loadInitialData(): void {
    this.loading = true;

    forkJoin({
      project: this.fetchProject(),
      members: this.fetchMembers()
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: this.handleInitialDataSuccess.bind(this),
        error: this.handleInitialDataError.bind(this)
      });
  }

  private fetchProject() {
    return this.projectService.getProjectById(this.projectId).pipe(
      catchError(error => {
        console.error('Project fetch error:', error);
        return of(null);
      })
    );
  }

  private fetchMembers() {
    return this.projectService.getMembers(this.projectId).pipe(
      catchError(error => {
        console.error('Members fetch error:', error);
        return of([]);
      })
    );
  }

  private handleInitialDataSuccess({ project, members }: { project: Project | null; members: Member[] }): void {
    if (!project) {
      this.showSnackBar('Project not found', 'error');
      return;
    }

    this.project = project;
    this.members = members;
  }

  private handleInitialDataError(error: any): void {
    console.error('Failed to load project data:', error);
    this.showSnackBar('Failed to load project data', 'error');
  }

  // === Member Management ===

  loadMembers(): void {
    this.loadingMembers = true;

    this.projectService.getMembers(this.projectId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingMembers = false)
      )
      .subscribe({
        next: members => this.members = members,
        error: error => {
          console.error('Failed to load members:', error);
          this.showSnackBar('Failed to load members', 'error');
        }
      });
  }

  inviteMember(): void {
    if (!this.validateInviteEmail()) return;

    this.inviting = true;

    this.projectService.inviteMember(this.projectId, this.inviteEmail.trim())
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.inviting = false)
      )
      .subscribe({
        next: this.handleInviteSuccess.bind(this),
        error: this.handleInviteError.bind(this)
      });
  }

  private validateInviteEmail(): boolean {
    const email = this.inviteEmail.trim();

    if (!email || !this.isValidEmail(email)) {
      this.showSnackBar('Please enter a valid email address', 'error');
      return false;
    }

    if (this.isEmailAlreadyMember(email)) {
      this.showSnackBar('This user is already a member', 'error');
      return false;
    }

    return true;
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  private isEmailAlreadyMember(email: string): boolean {
    return this.members.some(
      member => member.email.toLowerCase() === email.toLowerCase()
    );
  }

  private handleInviteSuccess(response: any): void {
    const message = response.userExists
      ? 'User added successfully'
      : 'Invitation sent successfully';

    this.showSnackBar(message, 'success');
    this.closeAddMemberModal();

    if (response.userExists) {
      this.loadMembers();
    }
  }

  private handleInviteError(error: any): void {
    const message = error.error?.message || 'Failed to send invitation';
    this.showSnackBar(message, 'error');
  }

  removeMember(userId: number, memberName: string): void {
    if (!this.confirmRemoval(memberName)) return;

    this.projectService.removeMember(this.projectId, userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.handleRemoveMemberSuccess(userId, memberName),
        error: this.handleRemoveMemberError.bind(this)
      });
  }

  private confirmRemoval(memberName: string): boolean {
    return confirm(`Remove ${memberName} from this project?`);
  }

  private handleRemoveMemberSuccess(userId: number, memberName: string): void {
    this.members = this.members.filter(m => m.userId !== userId);
    this.showSnackBar(`${memberName} removed successfully`, 'success');
  }

  private handleRemoveMemberError(error: any): void {
    const message = error.error?.message || 'Failed to remove member';
    console.error('Remove member error:', error);
    this.showSnackBar(message, 'error');
  }

  // === UI State Management ===

  toggleMembersPanel(): void {
    this.showMembersPanel = !this.showMembersPanel;

    if (this.showAddMemberModal) {
      this.closeAddMemberModal();
    }
  }

  openAddMemberModal(): void {
    this.showAddMemberModal = true;
    this.inviteEmail = '';
  }

  closeAddMemberModal(): void {
    this.showAddMemberModal = false;
    this.inviteEmail = '';
  }

  // === Permission Checks ===

  isOwner(): boolean {
    return this.project?.ownerId === this.currentUserId;
  }

  isAdmin(): boolean {
    return this.authService.getCurrentUser()?.role === 'ADMIN';
  }

  canManageMembers(): boolean {
    return this.isOwner() || this.isAdmin();
  }

  canRemoveMember(member: Member): boolean {
    return this.isOwner() && member.userId !== this.project?.ownerId;
  }

  // === Display Helpers ===

  getMemberInitials(member: Member): string {
    return this.getInitials(member.firstName, member.lastName);
  }

  getUserInitials(user: User): string {
    return this.getInitials(user.firstName, user.lastName);
  }

  private getInitials(firstName: string, lastName: string): string {
    const first = firstName?.charAt(0)?.toUpperCase() || '';
    const last = lastName?.charAt(0)?.toUpperCase() || '';
    return `${first}${last}` || '?';
  }

  getAvatarColor(userId: number): string {
    return this.avatarColors[userId % this.avatarColors.length];
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase().replace('_', '-')}`;
  }

  getMemberFullName(member: Member): string {
    return `${member.firstName} ${member.lastName}`.trim();
  }

  // === TrackBy Functions ===

  trackByUserId(_: number, member: Member): number {
    return member.userId;
  }

  trackByUserIdFn(_: number, user: User): number {
    return user.id;
  }

  // === Utility Methods ===

  private showSnackBar(message: string, type: SnackBarType): void {
    this.snackBar.open(message, 'Close', {
      duration: type === 'error' ? 5000 : 3000,
      panelClass: `snackbar-${type}`,
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }
}
