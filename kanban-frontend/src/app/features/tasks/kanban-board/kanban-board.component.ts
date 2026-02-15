import {Component, Input, OnInit} from '@angular/core';
import {CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem} from '@angular/cdk/drag-drop';
import {Task, TaskStatus} from '../../../core/models/task.model';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatMenuModule} from '@angular/material/menu';
import {TaskCardComponent} from '../task-card/task-card.component';
import {TaskService} from '../../../core/services/task.service';
import {TaskStatusService} from '../../../core/services/task-status.service';
import {MatDialog} from '@angular/material/dialog';
import {TaskDetailComponent} from '../task-detail/task-detail.component';
import {TaskFormComponent} from '../task-form/task-form.component';
import {StatusFormDialogComponent} from '../status-form-dialog/status-form-dialog.component';
import {ConfirmDialogComponent} from '../../../shared/components/confirm-dialog/confirm-dialog.component';

interface KanbanColumn {
  status: TaskStatus;  // Changed: now it's the full status object
  tasks: Task[];
}

@Component({
  selector: 'app-kanban-board',
  standalone: true,
  imports: [
    CommonModule,
    DragDropModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatMenuModule,
    TaskCardComponent
  ],
  templateUrl: './kanban-board.component.html',
  styleUrls: ['./kanban-board.component.css']
})
export class KanbanBoardComponent implements OnInit {
  @Input() projectId!: number;

  loading = true;
  columns: KanbanColumn[] = [];
  allStatuses: TaskStatus[] = [];

  constructor(
    private taskService: TaskService,
    private taskStatusService: TaskStatusService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadStatuses();
  }

  /**
   * Load all statuses for the project
   * If no statuses exist, they will be created automatically by backend
   */
  loadStatuses(): void {
    this.loading = true;
    this.taskStatusService.getProjectStatuses(this.projectId).subscribe({
      next: (statuses) => {
        this.allStatuses = statuses;
        this.initializeColumns(statuses);
        this.loadTasks();
      },
      error: (error) => {
        console.error('Error loading statuses:', error);
        this.snackBar.open('Error loading board statuses', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  /**
   * Initialize columns from statuses
   */
  initializeColumns(statuses: TaskStatus[]): void {
    this.columns = statuses.map(status => ({
      status: status,
      tasks: []
    }));
  }

  /**
   * Load all tasks for the project
   */
  loadTasks(): void {
    this.taskService.getAllTasks(this.projectId).subscribe({
      next: (response) => {
        this.distributeTasks(response.content);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading tasks:', error);
        this.snackBar.open('Error loading tasks', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  /**
   * Distribute tasks into their respective columns
   */
  distributeTasks(tasks: Task[]): void {
    // Clear all columns
    this.columns.forEach(col => col.tasks = []);

    // Distribute tasks to columns based on status ID
    tasks.forEach(task => {
      const column = this.columns.find(col => col.status.id === task.status.id);
      if (column) {
        column.tasks.push(task);
      }
    });

    // Sort tasks within each column by position
    this.columns.forEach(col => {
      col.tasks.sort((a, b) => (a.position || 0) - (b.position || 0));
    });
  }

  /**
   * Handle drag and drop events
   */
  drop(event: CdkDragDrop<Task[]>, targetColumn: KanbanColumn): void {
    if (event.previousContainer === event.container) {
      // Just reordering within the same column
      moveItemInArray(
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
      this.updateTaskPositions(event.container.data);
    } else {
      // Moving task to a different column
      const task = event.previousContainer.data[event.previousIndex];
      const oldStatus = task.status;
      const sourceColumn = event.previousContainer.data;
      const targetColumnData = event.container.data;
      const oldIndex = event.previousIndex;

      // OPTIMISTIC UPDATE: Update task status immediately
      task.status = targetColumn.status;

      // Transfer the item visually
      transferArrayItem(
        sourceColumn,
        targetColumnData,
        event.previousIndex,
        event.currentIndex
      );

      // Update positions in target column
      this.updateTaskPositions(targetColumnData);

      // Update on backend
      this.taskService.updateTaskStatus(
        task.id,
        targetColumn.status.id,  // Send status ID
        this.projectId,
        event.currentIndex  // Send position
      ).subscribe({
        next: (updatedTask) => {
          // Merge the updated task data from server
          if (updatedTask) {
            Object.assign(task, updatedTask);
          }

          this.snackBar.open(`Task moved to ${targetColumn.status.name}`, 'Close', {
            duration: 2000,
            horizontalPosition: 'end',
            verticalPosition: 'bottom'
          });
        },
        error: (error) => {
          console.error('Failed to update task status:', error);

          // ROLLBACK: Revert the optimistic update
          task.status = oldStatus;

          // Move the task back to its original position
          const currentIndex = targetColumnData.indexOf(task);
          if (currentIndex !== -1) {
            transferArrayItem(
              targetColumnData,
              sourceColumn,
              currentIndex,
              oldIndex
            );
          }

          this.snackBar.open('Failed to move task', 'Close', {
            duration: 3000,
            panelClass: 'snackbar-error'
          });
        }
      });
    }
  }

  /**
   * Update positions of tasks in a column
   */
  updateTaskPositions(tasks: Task[]): void {
    tasks.forEach((task, index) => {
      task.position = index;
    });
  }

  /**
   * Get connected drop list IDs for drag and drop
   */
  getConnectedLists(): string[] {
    return this.columns.map((_, index) => `cdk-drop-list-${index}`);
  }

  /**
   * Open dialog to create a new task in a specific column
   */
  createTask(column: KanbanColumn): void {
    const dialogRef = this.dialog.open(TaskFormComponent, {
      width: '600px',
      data: {
        projectId: this.projectId,
        defaultStatusId: column.status.id  // Pass the status ID
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTasks(); // Reload tasks after creation
      }
    });
  }

  /**
   * Open task detail dialog
   */
  onTaskClick(task: Task): void {
    const dialogRef = this.dialog.open(TaskDetailComponent, {
      width: '700px',
      maxWidth: '90vw',
      data: { task, projectId: this.projectId }
    });

    dialogRef.afterClosed().subscribe(result => {
      // Reload tasks if deleted or updated
      if (result?.deleted || result?.updated) {
        this.loadTasks();
      }
    });
  }

  // ===== STATUS MANAGEMENT METHODS =====

  /**
   * Open dialog to create a new status column
   */
  createStatus(): void {
    const dialogRef = this.dialog.open(StatusFormDialogComponent, {
      width: '500px',
      data: { projectId: this.projectId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadStatuses(); // Reload statuses and tasks
        this.snackBar.open('Status column created!', 'Close', { duration: 2000 });
      }
    });
  }

  /**
   * Open dialog to edit a status
   */
  editStatus(status: TaskStatus, event: Event): void {
    event.stopPropagation();

    const dialogRef = this.dialog.open(StatusFormDialogComponent, {
      width: '500px',
      data: { projectId: this.projectId, status: status }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadStatuses();
        this.snackBar.open('Status updated!', 'Close', { duration: 2000 });
      }
    });
  }

  /**
   * Delete a status and move its tasks
   */
  deleteStatus(column: KanbanColumn, event: Event): void {
    event.stopPropagation();

    if (this.columns.length <= 1) {
      this.snackBar.open('Cannot delete the last status', 'Close', { duration: 3000 });
      return;
    }

    // Find other statuses to move tasks to
    const otherStatuses = this.allStatuses.filter(s => s.id !== column.status.id);

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      data: {
        title: 'Delete Status Column',
        message: `Are you sure you want to delete "${column.status.name}"? ` +
          `All ${column.tasks.length} task(s) will be moved to another status.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        showMoveToSelect: true,
        moveToOptions: otherStatuses
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.confirmed && result?.moveToStatusId) {
        this.taskStatusService.deleteStatus(
          column.status.id,
          result.moveToStatusId,
          this.projectId
        ).subscribe({
          next: () => {
            this.snackBar.open('Status deleted successfully', 'Close', { duration: 2000 });
            this.loadStatuses();
          },
          error: (error) => {
            console.error('Error deleting status:', error);
            this.snackBar.open(
              error.error?.message || 'Failed to delete status',
              'Close',
              { duration: 3000 }
            );
          }
        });
      }
    });
  }
}
