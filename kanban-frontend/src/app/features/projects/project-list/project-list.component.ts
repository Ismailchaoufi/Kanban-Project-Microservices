import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatChipsModule} from '@angular/material/chips';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {Project} from '../../../core/models/project.model';
import {ProjectService} from '../../../core/services/project.service';
import {Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {ProjectFormComponent} from '../project-form/project-form.component';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './project-list.component.html',
  styleUrl: './project-list.component.css'
})
export class ProjectListComponent implements OnInit {
  projects: Project[] = [];
  loading = true;

  constructor(
    private projectService: ProjectService,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (response) => {
        this.projects = response.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  viewProject(projectId: number): void {
    this.router.navigate(['/projects', projectId]);
  }
  // Méthode pour créer un projet
  createNewProject(): void {
    const dialogRef = this.dialog.open(ProjectFormComponent, {
      width: '600px',
      data: {}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadProjects(); // Recharger la liste
      }
    });
  }

// Méthode pour éditer un projet
  editProject(project: Project, event: Event): void {
    event.stopPropagation(); // Empêcher la navigation

    const dialogRef = this.dialog.open(ProjectFormComponent, {
      width: '600px',
      data: { project }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadProjects();
      }
    });
  }
}
