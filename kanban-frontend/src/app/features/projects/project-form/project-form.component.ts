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
import { ProjectService } from '../../../core/services/project.service';
import { Project, ProjectStatus, ProjectRequest } from '../../../core/models/project.model';

@Component({
  selector: 'app-project-form',
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
  templateUrl: './project-form.component.html',
  styleUrls: ['./project-form.component.css']
})
export class ProjectFormComponent implements OnInit {
  projectForm!: FormGroup;
  loading = false;
  isEditMode = false;

  statusOptions = [
    { value: ProjectStatus.IN_PROGRESS, label: 'In Progress' },
    { value: ProjectStatus.PAUSED, label: 'Paused' },
    { value: ProjectStatus.COMPLETED, label: 'Completed' }
  ];

  colorOptions = [
    { value: '#1976d2', label: 'Blue' },
    { value: '#388e3c', label: 'Green' },
    { value: '#f57c00', label: 'Orange' },
    { value: '#d32f2f', label: 'Red' },
    { value: '#7b1fa2', label: 'Purple' },
    { value: '#0097a7', label: 'Cyan' }
  ];

  constructor(
    private fb: FormBuilder,
    private projectService: ProjectService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<ProjectFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { project?: Project }
  ) {}

  ngOnInit(): void {
    this.isEditMode = !!this.data?.project;
    this.initForm();
  }

  initForm(): void {
    this.projectForm = this.fb.group({
      title: [this.data?.project?.title || '', [Validators.required, Validators.minLength(3)]],
      description: [this.data?.project?.description || ''],
      status: [this.data?.project?.status || ProjectStatus.IN_PROGRESS],
      color: [this.data?.project?.color || '#1976d2'],
      startDate: [this.data?.project?.startDate || null],
      endDate: [this.data?.project?.endDate || null]
    });
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      return;
    }

    this.loading = true;
    const formData: ProjectRequest = this.projectForm.value;

    const operation = this.isEditMode
      ? this.projectService.updateProject(this.data.project!.id, formData)
      : this.projectService.createProject(formData);

    operation.subscribe({
      next: (project) => {
        this.snackBar.open(
          this.isEditMode ? 'Project updated successfully' : 'Project created successfully',
          'Close',
          { duration: 3000 }
        );
        this.dialogRef.close(project);
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
