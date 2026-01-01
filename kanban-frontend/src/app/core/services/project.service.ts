import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  Project,
  ProjectRequest,
  Member,
  ProjectStats
} from '../models/project.model';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = `${environment.apiUrl}/projects`;
  private projectsSubject = new BehaviorSubject<Project[]>([]);
  public projects$ = this.projectsSubject.asObservable();

  constructor(private http: HttpClient) {}

  getAllProjects(page: number = 0, size: number = 20): Observable<PageResponse<Project>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<Project>>(this.apiUrl, { params })
      .pipe(
        tap(response => this.projectsSubject.next(response.content))
      );
  }

  getProjectById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.apiUrl}/${id}`);
  }

  createProject(request: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, request)
      .pipe(
        tap(() => this.refreshProjects())
      );
  }

  updateProject(id: number, request: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.apiUrl}/${id}`, request)
      .pipe(
        tap(() => this.refreshProjects())
      );
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`)
      .pipe(
        tap(() => this.refreshProjects())
      );
  }

  addMember(projectId: number, userId: number): Observable<Member> {
    return this.http.post<Member>(`${this.apiUrl}/${projectId}/members`, { userId });
  }

  getMembers(projectId: number): Observable<Member[]> {
    return this.http.get<Member[]>(`${this.apiUrl}/${projectId}/members`);
  }

  removeMember(projectId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${projectId}/members/${userId}`);
  }

  getProjectStats(projectId: number): Observable<ProjectStats> {
    return this.http.get<ProjectStats>(`${this.apiUrl}/${projectId}/stats`);
  }

  private refreshProjects(): void {
    this.getAllProjects().subscribe();
  }
}
