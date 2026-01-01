import { Component, OnInit, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TaskService } from '../../../core/services/task.service';
import { ProjectService } from '../../../core/services/project.service';
import { Task, TaskStatus, Priority, TaskRequest } from '../../../core/models/task.model';
import { Member } from '../../../core/models/project.model';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatDatepickerModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './task-form.component.html',
  styleUrls: ['./task-form.component.css']
})
export class TaskFormComponent implements OnInit {
  taskForm!: FormGroup;
  loading = false;
  isEditMode = false;
  members: Member[] = [];

  statusOptions = [
    { value: TaskStatus.TODO, label: 'To Do' },
    { value: TaskStatus.IN_PROGRESS, label: 'In Progress' },
    { value: TaskStatus.DONE, label: 'Done' }
  ];

  priorityOptions = [
    { value: Priority.LOW, label: 'Low' },
    { value: Priority.MEDIUM, label: 'Medium' },
    { value: Priority.HIGH, label: 'High' }
  ];

  constructor(
    private fb: FormBuilder,
    private taskService: TaskService,
    private projectService: ProjectService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<TaskFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { task?: Task; projectId: number }
  ) {}

  ngOnInit(): void {
    this.isEditMode = !!this.data?.task;
    this.initForm();
    this.loadProjectMembers();
  }

  initForm(): void {
    this.taskForm = this.fb.group({
      title: [this.data?.task?.title || '', [Validators.required, Validators.minLength(3)]],
      description: [this.data?.task?.description || ''],
      status: [this.data?.task?.status || TaskStatus.TODO],
      priority: [this.data?.task?.priority || Priority.MEDIUM],
      dueDate: [this.data?.task?.dueDate || null],
      assignedTo: [this.data?.task?.assignedUser?.id || null]
    });
  }

  loadProjectMembers(): void {
    this.projectService.getMembers(this.data.projectId).subscribe({
      next: (members) => {
        this.members = members;
      },
      error: (error) => {
        console.error('Error loading members:', error);
      }
    });
  }

  onSubmit(): void {
    if (this.taskForm.invalid) {
      return;
    }

    this.loading = true;
    const formData: TaskRequest = {
      ...this.taskForm.value,
      projectId: this.data.projectId
    };

    const operation = this.isEditMode
      ? this.taskService.updateTask(this.data.task!.id, formData)
      : this.taskService.createTask(formData);

    operation.subscribe({
      next: (task) => {
        this.snackBar.open(
          this.isEditMode ? 'Task updated successfully' : 'Task created successfully',
          'Close',
          { duration: 3000 }
        );
        this.dialogRef.close(task);
      },
      error: (error) => {
        this.loading = false;
        this.snackBar.open(error.message || 'Operation failed', 'Close', { duration: 3000 });
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
