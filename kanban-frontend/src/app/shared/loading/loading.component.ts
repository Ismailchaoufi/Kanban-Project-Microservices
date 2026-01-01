import { Component, Input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    <div class="loading-container">
      <mat-spinner [diameter]="size"></mat-spinner>
      <p class="loading-message">{{ message }}</p>
    </div>
  `,
  styles: [`
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px;
      min-height: 200px;
    }
    .loading-message {
      margin-top: 20px;
      color: #666;
      font-size: 14px;
    }
  `]
})
export class LoadingComponent {
  @Input() message: string = 'Loading...';
  @Input() size: number = 50;
}
