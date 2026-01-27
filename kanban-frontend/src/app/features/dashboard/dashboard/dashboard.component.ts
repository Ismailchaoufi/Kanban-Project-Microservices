import {Component, OnInit, ViewChild, ChangeDetectorRef, AfterViewInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatChipsModule} from '@angular/material/chips';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {BaseChartDirective} from 'ng2-charts';
import {Project} from '../../../core/models/project.model';
import {ChartData, ChartOptions} from 'chart.js';
import {ProjectService} from '../../../core/services/project.service';
import {TaskService} from '../../../core/services/task.service';
import {AuthService} from '../../../core/services/auth.service';
import {Router} from '@angular/router';
import {Task} from '../../../core/models/task.model';
import {ProjectFormComponent} from '../../projects/project-form/project-form.component';
import {MatDialog} from '@angular/material/dialog';

// ✅ IMPORTANT: Import ALL necessary Chart.js components
import {
  Chart,
  ArcElement,
  Tooltip,
  Legend,
  PieController,  // This is REQUIRED for pie charts!
  CategoryScale,
  LinearScale
} from 'chart.js';

// ✅ Register ALL Chart.js components
Chart.register(
  ArcElement,
  Tooltip,
  Legend,
  PieController,  // Must register the PieController!
  CategoryScale,
  LinearScale
);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    BaseChartDirective
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, AfterViewInit {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  loading = true;
  userName = '';

  totalProjects = 0;
  totalTasks = 0;
  todoTasks = 0;
  inProgressTasks = 0;
  doneTasks = 0;

  recentProjects: Project[] = [];
  myTasks: Task[] = [];

  // Initialize with empty data
  public pieChartData: ChartData<'pie'> = {
    labels: ['To Do', 'In Progress', 'Done'],
    datasets: [{
      data: [0, 0, 0],
      backgroundColor: ['#ff9800', '#2196f3', '#4caf50'],
      borderWidth: 2,
      borderColor: '#ffffff',
      hoverOffset: 4
    }]
  };

  public pieChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          padding: 15,
          font: {
            size: 12
          }
        }
      },
      tooltip: {
        enabled: true,
        callbacks: {
          label: function(context) {
            const label = context.label || '';
            const value = context.parsed || 0;
            const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0);
            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0';
            return `${label}: ${value} (${percentage}%)`;
          }
        }
      }
    }
  };

  constructor(
    private projectService: ProjectService,
    private taskService: TaskService,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.userName = user?.firstName || 'User';
    this.loadDashboardData();
  }

  ngAfterViewInit(): void {
    // Don't try to update chart here - data isn't loaded yet
    // The chart will update when data loads in loadTasksForDashboard()
  }

  loadDashboardData(): void {
    this.loading = true;

    this.projectService.getAllProjects(0, 10).subscribe({
      next: (response) => {
        this.recentProjects = response.content.slice(0, 5);
        this.totalProjects = response.totalElements;

        // Always load tasks
        this.loadTasksForDashboard();
      },
      error: (err) => {
        console.error('Error loading projects:', err);
        this.loadTasksForDashboard(); // Still try to load tasks
      }
    });
  }

  loadTasksForDashboard(): void {
    const userId = this.authService.getCurrentUser()?.id;

    if (!userId) {
      this.loading = false;
      this.cdr.detectChanges();
      return;
    }

    this.taskService.getAllTasks(undefined, undefined, undefined, userId, undefined, 0, 100).subscribe({
      next: (response) => {
        const allTasks = response.content;

        // Update all task counts
        this.totalTasks = allTasks.length;
        this.todoTasks = allTasks.filter(t => t.status === 'TODO').length;
        this.inProgressTasks = allTasks.filter(t => t.status === 'IN_PROGRESS').length;
        this.doneTasks = allTasks.filter(t => t.status === 'DONE').length;
        this.myTasks = allTasks.slice(0, 5);

        console.log('Tasks loaded:', {
          total: this.totalTasks,
          todo: this.todoTasks,
          inProgress: this.inProgressTasks,
          done: this.doneTasks,
          myTasks: this.myTasks.length
        });

        // Update chart data
        this.updateChartData();

        // Mark loading as complete
        this.loading = false;

        // Force Angular to detect changes
        this.cdr.detectChanges();

        // Give the DOM time to render, then update chart
        setTimeout(() => {
          this.forceChartUpdate();
        }, 150);
      },
      error: (err) => {
        console.error('Error loading tasks:', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  updateChartData(): void {
    // Create a completely NEW object (don't mutate)
    this.pieChartData = {
      labels: ['To Do', 'In Progress', 'Done'],
      datasets: [{
        data: [this.todoTasks, this.inProgressTasks, this.doneTasks],
        backgroundColor: ['#ff9800', '#2196f3', '#4caf50'],
        borderWidth: 2,
        borderColor: '#ffffff',
        hoverOffset: 4
      }]
    };

    console.log('Chart data updated:', this.pieChartData);
  }

  forceChartUpdate(): void {
    if (this.chart) {
      try {
        // Try multiple update methods
        this.chart.chart?.update('active');
        this.chart.update();
        console.log('Chart updated successfully');
      } catch (error) {
        console.error('Error updating chart:', error);
      }
    } else {
      console.warn('Chart not found in ViewChild');
    }
  }

  getCompletionPercentage(): number {
    if (this.totalTasks === 0) return 0;
    return Math.round((this.doneTasks / this.totalTasks) * 100);
  }

  navigateToProject(projectId: number): void {
    this.router.navigate(['/projects', projectId]);
  }

  createNewProject(): void {
    const dialogRef = this.dialog.open(ProjectFormComponent, {
      width: '600px',
      data: {}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Reload dashboard data after creating project
        this.loadDashboardData();
      }
    });
  }

  // TrackBy function for better performance
  trackByTaskId(index: number, task: Task): number {
    return task.id;
  }
}
