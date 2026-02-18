import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatChipsModule} from '@angular/material/chips';
import {MatIconModule} from '@angular/material/icon';
import {Task} from '../../../core/models/task.model';

@Component({
  selector: 'app-task-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule
  ],
  templateUrl: './task-card.component.html',
  styleUrls: ['./task-card.component.css']
})
export class TaskCardComponent {
  @Input() task!: Task;

  getPriorityColor(): string {
    switch (this.task.priority) {
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

  isOverdue(): boolean {
    if (!this.task.dueDate) return false;
    return new Date(this.task.dueDate) < new Date() && this.task.status.name.toLowerCase().includes('done');
  }
}
