import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {TaskStatus} from '../../../../core/models/task.model';

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
  imports: [],
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
