import {Component, Inject} from '@angular/core';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {TaskStatusService} from '../../../../core/services/task-status-service.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import {TaskStatus, TaskStatusRequest} from '../../../../core/models/task.model';
import {MatError, MatFormField, MatInput} from '@angular/material/input';
import {MatIcon} from '@angular/material/icon';
import {MatButton} from '@angular/material/button';
import {NgForOf, NgIf} from '@angular/common';

export interface StatusFormDialogData {
  projectId: number;
  status?: TaskStatus;
}

@Component({
  selector: 'app-status-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatError,
    MatFormField,
    MatDialogContent,
    MatDialogActions,
    MatIcon,
    MatInput,
    MatDialogTitle,
    MatButton,
    FormsModule,
    NgIf,
    NgForOf
  ],
  templateUrl: './status-form-dialog.component.html',
  standalone: true,
  styleUrl: './status-form-dialog.component.css'
})
export class StatusFormDialogComponent {
  form: FormGroup;
  isEditing: boolean;
  loading = false;

  // Predefined color options
  colorOptions = [
    '#ff9800',  // Orange (To Do)
    '#2196f3',  // Blue (In Progress)
    '#4caf50',  // Green (Done)
    '#9c27b0',  // Purple (Review)
    '#f44336',  // Red (Blocked)
    '#00bcd4',  // Cyan (Testing)
    '#ff5722',  // Deep Orange
    '#3f51b5',  // Indigo
    '#e91e63',  // Pink
    '#8bc34a',  // Light Green
    '#ffc107',  // Amber
    '#607d8b'   // Blue Grey
  ];

  constructor(
    private fb: FormBuilder,
    private taskStatusService: TaskStatusService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<StatusFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: StatusFormDialogData
  ) {
    this.isEditing = !!data.status;
    this.form = this.fb.group({
      name: [data.status?.name || '', Validators.required],
      color: [data.status?.color || '#ff9800', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]]
    });
  }

  ngOnInit(): void {}

  selectColor(color: string): void {
    this.form.patchValue({ color });
  }

  onColorInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.form.patchValue({ color: input.value });
  }

  onSubmit(): void {
    if (!this.form.valid) return;

    this.loading = true;
    const request: TaskStatusRequest = this.form.value;

    const operation = this.isEditing
      ? this.taskStatusService.updateStatus(this.data.projectId, this.data.status!.id, request)
      : this.taskStatusService.createStatus(this.data.projectId, request);

    operation.subscribe({
      next: () => {
        this.dialogRef.close(true);
      },
      error: (error) => {
        console.error('Error saving status:', error);
        this.snackBar.open(
          error.error?.message || 'Erreur lors de la sauvegarde',
          'Fermer',
          { duration: 3000 }
        );
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
