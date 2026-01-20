import {Component, Input, OnInit} from '@angular/core';
import {CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem} from '@angular/cdk/drag-drop';
import {Task, TaskStatus} from '../../../core/models/task.model';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {TaskCardComponent} from '../task-card/task-card.component';
import {TaskService} from '../../../core/services/task.service';
import {MatDialog} from '@angular/material/dialog';
import {TaskDetailComponent} from '../task-detail/task-detail.component';
import {TaskFormComponent} from '../task-form/task-form.component';

interface KanbanColumn {
  id: TaskStatus;
  title: string;
  tasks: Task[];
  color: string;
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
    TaskCardComponent
  ],
  templateUrl: './kanban-board.component.html',
  styleUrls: ['./kanban-board.component.css']
})
export class KanbanBoardComponent implements OnInit {
  @Input() projectId!: number;

  loading = true;
  columns: KanbanColumn[] = [
    {
      id: TaskStatus.TODO,
      title: 'To Do',
      tasks: [],
      color: '#ff9800'
    },
    {
      id: TaskStatus.IN_PROGRESS,
      title: 'In Progress',
      tasks: [],
      color: '#2196f3'
    },
    {
      id: TaskStatus.DONE,
      title: 'Done',
      tasks: [],
      color: '#4caf50'
    }
  ];

  constructor(
    private taskService: TaskService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.taskService.getAllTasks(this.projectId).subscribe({
      next: (response) => {
        this.distributeTasks(response.content);
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Error loading tasks', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  distributeTasks(tasks: Task[]): void {
    this.columns.forEach(col => col.tasks = []);
    tasks.forEach(task => {
      const column = this.columns.find(col => col.id === task.status);
      if (column) {
        column.tasks.push(task);
      }
    });
  }

  drop(event: CdkDragDrop<Task[]>, targetStatus: TaskStatus): void {
    if (event.previousContainer === event.container) {
      // Just reordering within the same column
      moveItemInArray(
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
    } else {
      // Moving task to a different column
      const task = event.previousContainer.data[event.previousIndex];
      const oldStatus = task.status;
      const sourceColumn = event.previousContainer.data;
      const targetColumn = event.container.data;
      const oldIndex = event.previousIndex;

      // OPTIMISTIC UPDATE: Update task status immediately
      task.status = targetStatus;

      // Transfer the item visually
      transferArrayItem(
        sourceColumn,
        targetColumn,
        event.previousIndex,
        event.currentIndex
      );

      // Update on backend
      this.taskService.updateTaskStatus(task.id, targetStatus, this.projectId).subscribe({
        next: (updatedTask) => {
          // Merge the updated task data from server
          if (updatedTask) {
            Object.assign(task, updatedTask);
          }

          this.snackBar.open('Task status updated', 'Close', {
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
          const currentIndex = targetColumn.indexOf(task);
          if (currentIndex !== -1) {
            transferArrayItem(
              targetColumn,
              sourceColumn,
              currentIndex,
              oldIndex
            );
          }

          this.snackBar.open('Failed to update task status', 'Close', {
            duration: 3000,
            panelClass: 'snackbar-error'
          });
        }
      });
    }
  }

  getConnectedLists(): string[] {
    return this.columns.map((_, index) => `cdk-drop-list-${index}`);
  }

  createTask(): void {
    const dialogRef = this.dialog.open(TaskFormComponent, {
      width: '600px',
      data: { projectId: this.projectId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTasks(); // Reload tasks after creation
      }
    });
  }

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
}
