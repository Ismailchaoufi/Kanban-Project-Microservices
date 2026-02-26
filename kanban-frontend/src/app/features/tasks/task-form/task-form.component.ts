
import { TaskService } from '../../../core/services/task.service';
import { ProjectService } from '../../../core/services/project.service';
import { Member } from '../../../core/models/project.model';
import {Priority, Task, TaskRequest, TaskStatus} from '../../../core/models/task.model';
import {TaskStatusService} from '../../../core/services/task-status-service.service';
import {Component, Inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {MatSelectModule} from '@angular/material/select';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';

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

  statusOptions: TaskStatus[] = [];

  priorityOptions = [
    { value: Priority.LOW, label: 'Low' },
    { value: Priority.MEDIUM, label: 'Medium' },
    { value: Priority.HIGH, label: 'High' }
  ];

  constructor(
    private fb: FormBuilder,
    private taskService: TaskService,
    private projectService: ProjectService,
    private taskStatusService: TaskStatusService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<TaskFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { task?: Task; projectId: number; defaultStatusId?: number }
  ) {}

  ngOnInit(): void {
    this.isEditMode = !!this.data?.task;
    this.initForm();
    this.loadStatuses();
    this.loadProjectMembers();
  }

  loadStatuses(): void {
    this.taskStatusService.getProjectStatuses(this.data.projectId).subscribe({
      next: (statuses) => {
        this.statusOptions = statuses;
        const currentStatusId = this.taskForm.get('statusId')?.value;
        if (!currentStatusId && statuses.length > 0) {
          this.taskForm.patchValue({ statusId: statuses[0].id });
        }
      },
      error: (error) => {
        console.error('Error loading statuses:', error);
      }
    });
  }

  initForm(): void {
    const defaultStatusId = this.data?.defaultStatusId
      || this.data?.task?.status?.id
      || (this.statusOptions.length > 0 ? this.statusOptions[0].id : null);

    this.taskForm = this.fb.group({
      title: [this.data?.task?.title || '', [Validators.required, Validators.minLength(3)]],
      description: [this.data?.task?.description || ''],
      statusId: [defaultStatusId],
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
    const trimmedTitle = (this.taskForm.get('title')?.value || '').trim();
    this.taskForm.patchValue({ title: trimmedTitle });
    if (this.taskForm.invalid) {
      this.taskForm.markAllAsTouched();
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
