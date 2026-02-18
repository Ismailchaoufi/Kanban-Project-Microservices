import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Task } from '../../../core/models/task.model';
import { TaskService } from '../../../core/services/task.service';
import { TaskFormComponent } from '../task-form/task-form.component';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatSnackBarModule
  ],
  templateUrl: './task-detail.component.html',
  styleUrls: ['./task-detail.component.css']
})
export class TaskDetailComponent {

  constructor(
    public dialogRef: MatDialogRef<TaskDetailComponent>,
    private dialog: MatDialog,
    private taskService: TaskService,
    private snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) public data: { task: Task; projectId: number }
  ) {}

  getPriorityColor(): string {
    switch (this.data.task.priority) {
      case 'HIGH':
        return '#f44336';
      case 'MEDIUM':
        return '#ff9800';
      case 'LOW':
        return '#9e9e9e';
      default:
        return '#9e9e9e';
    }
  }

  getStatusColor(): string {
    return this.data.task.status.color;
  }

  isOverdue(): boolean {
    if (!this.data.task.dueDate) return false;
    return new Date(this.data.task.dueDate) < new Date() && this.data.task.status.name.toLowerCase().includes('done');
  }

  onEdit(): void {
    const dialogRef = this.dialog.open(TaskFormComponent, {
      width: '600px',
      data: { task: this.data.task, projectId: this.data.projectId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Update the local task with all new data from server
        Object.assign(this.data.task, result);

        // Notify parent component to refresh
        this.dialogRef.close({ updated: true, task: result });
      }
    });
  }

  onDelete(): void {
    if (confirm('Are you sure you want to delete this task?')) {
      this.taskService.deleteTask(this.data.task.id, this.data.projectId).subscribe({
        next: () => {
          this.snackBar.open('Task deleted successfully', 'Close', { duration: 3000 });
          this.dialogRef.close({ deleted: true });
        },
        error: (error) => {
          this.snackBar.open(error.message || 'Failed to delete task', 'Close', { duration: 3000 });
        }
      });
    }
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
