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
import { Chart, ArcElement, Tooltip, Legend } from 'chart.js';

// Register Chart.js components
Chart.register(ArcElement, Tooltip, Legend);

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

  public pieChartData: ChartData<'pie'> = {
    labels: ['To Do', 'In Progress', 'Done'],
    datasets: [{
      data: [0, 0, 0],
      backgroundColor: ['#ff9800', '#2196f3', '#4caf50'],
      borderWidth: 2,
      borderColor: '#ffffff'
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
    // Force initial chart render after view is initialized
    setTimeout(() => {
      if (this.chart && this.totalTasks > 0) {
        this.chart.update();
      }
    }, 100);
  }

  loadDashboardData(): void {
    this.loading = true;

    this.projectService.getAllProjects(0, 10).subscribe({
      next: (response) => {
        this.recentProjects = response.content.slice(0, 5);
        this.totalProjects = response.totalElements;

        // Always load tasks, even if no projects
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
        this.totalTasks = allTasks.length;

        // Calculate task counts
        this.todoTasks = allTasks.filter(t => t.status === 'TODO').length;
        this.inProgressTasks = allTasks.filter(t => t.status === 'IN_PROGRESS').length;
        this.doneTasks = allTasks.filter(t => t.status === 'DONE').length;

        // Set my tasks
        this.myTasks = allTasks.slice(0, 5);

        // Update chart with new data
        this.updateChart();

        this.loading = false;

        // Force change detection
        this.cdr.detectChanges();

        // Force chart update after a short delay to ensure DOM is ready
        setTimeout(() => {
          if (this.chart) {
            this.chart.update();
          }
        }, 100);
      },
      error: (err) => {
        console.error('Error loading tasks:', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  updateChart(): void {
    // Create new chart data object (important for change detection)
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

    // Trigger change detection
    this.cdr.detectChanges();

    // Update the chart if it exists
    if (this.chart) {
      this.chart.chart?.update();
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
