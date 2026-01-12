import {Component, OnInit} from '@angular/core';
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
// import { Chart, ArcElement, Tooltip, Legend } from 'chart.js';
//
// Chart.register(ArcElement, Tooltip, Legend);

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
export class DashboardComponent implements OnInit {
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
      backgroundColor: ['#ff9800', '#2196f3', '#4caf50']
    }]
  };

  public pieChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    plugins: {
      legend: {
        position: 'bottom'
      },
      tooltip: {
        enabled: true
      }
    }
  };

  constructor(
    private projectService: ProjectService,
    private taskService: TaskService,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.userName = user?.firstName || 'User';
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;
    this.projectService.getAllProjects(0, 10).subscribe({
      next: (response) => {
        this.recentProjects = response.content.slice(0, 5);
        this.totalProjects = response.totalElements;

        if (this.recentProjects.length > 0) {
          this.loadTasksForDashboard();
        } else {
          this.loading = false;
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadTasksForDashboard(): void {
    this.taskService.getAllTasks(undefined, undefined, undefined, undefined, undefined, 0, 100).subscribe({
      next: (response) => {
        const allTasks = response.content;
        this.totalTasks = allTasks.length;

        this.todoTasks = allTasks.filter(t => t.status === 'TODO').length;
        this.inProgressTasks = allTasks.filter(t => t.status === 'IN_PROGRESS').length;
        this.doneTasks = allTasks.filter(t => t.status === 'DONE').length;

        const userId = this.authService.getCurrentUser()?.id;
        this.myTasks = allTasks.filter(t => t.assignedUser?.id === userId).slice(0, 5);

        this.updateChart();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  updateChart(): void {
    this.pieChartData = {
      labels: ['To Do', 'In Progress', 'Done'],
      datasets: [{
        data: [this.todoTasks, this.inProgressTasks, this.doneTasks],
        backgroundColor: ['#ff9800', '#2196f3', '#4caf50']
      }]
    };
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
        // 👉 Redirection après création
        this.router.navigate(['/projects']);
      }
    });
  }
}
