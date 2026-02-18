import {Component, Inject} from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import {TaskStatus} from '../../../../core/models/task.model';
import {MatOption} from '@angular/material/core';
import {MatFormField} from '@angular/material/input';
import {MatSelect} from '@angular/material/select';
import {FormsModule} from '@angular/forms';
import {MatButton} from '@angular/material/button';
import {NgForOf, NgIf} from '@angular/common';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  showMoveToSelect?: boolean;
  moveToOptions?: TaskStatus[];
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [
    MatOption,
    MatFormField,
    MatDialogActions,
    MatDialogContent,
    MatSelect,
    FormsModule,
    MatButton,
    MatDialogTitle,
    NgForOf,
    NgIf
  ],
  templateUrl: './confirm-dialog.component.html',
  standalone: true,
  styleUrl: './confirm-dialog.component.css'
})
export class ConfirmDialogComponent {
  selectedMoveToId?: number;

  constructor(
    private dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {
    // Pre-select first option if available
    if (data.moveToOptions && data.moveToOptions.length > 0) {
      this.selectedMoveToId = data.moveToOptions[0].id;
    }
  }

  onConfirm(): void {
    this.dialogRef.close({
      confirmed: true,
      moveToStatusId: this.selectedMoveToId
    });
  }

  onCancel(): void {
    this.dialogRef.close({
      confirmed: false
    });
  }
}
