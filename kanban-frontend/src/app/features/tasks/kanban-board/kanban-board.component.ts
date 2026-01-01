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
      moveItemInArray(
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
    } else {
      const task = event.previousContainer.data[event.previousIndex];

      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );

      this.updateTaskStatus(task.id, targetStatus);
    }
  }

  updateTaskStatus(taskId: number, newStatus: TaskStatus): void {
    this.taskService.updateTaskStatus(taskId, newStatus, this.projectId).subscribe({
      next: () => {
        this.snackBar.open('Task status updated', 'Close', {
          duration: 2000,
          horizontalPosition: 'end',
          verticalPosition: 'bottom'
        });
      },
      error: () => {
        this.snackBar.open('Failed to update task status', 'Close', {
          duration: 3000
        });
        this.loadTasks();
      }
    });
  }

  getConnectedLists(): string[] {
    return this.columns.map((_, index) => `cdk-drop-list-${index}`);
  }

  // Méthode pour créer une tâche
  createTask(): void {
    const dialogRef = this.dialog.open(TaskFormComponent, {
      width: '600px',
      data: { projectId: this.projectId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTasks();
      }
    });
  }

// Méthode pour voir les détails
  onTaskClick(task: Task): void {
    const dialogRef = this.dialog.open(TaskDetailComponent, {
      width: '700px',
      maxWidth: '90vw',
      data: { task, projectId: this.projectId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.deleted) {
        this.loadTasks();
      }
    });
  }
}
