import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  Task,
  TaskRequest,
  TaskStatus,
  Priority,
  UpdateStatusRequest
} from '../models/task.model';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

interface TaskStats {
  projectId: number;
  totalTasks: number;
  todoTasks: number;
  inProgressTasks: number;
  doneTasks: number;
}

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private apiUrl = `${environment.apiUrl}/tasks`;
  private tasksSubject = new BehaviorSubject<Task[]>([]);
  public tasks$ = this.tasksSubject.asObservable();

  constructor(private http: HttpClient) {}

  getAllTasks(
    projectId?: number,
    status?: TaskStatus,
    priority?: Priority,
    assignedTo?: number,
    search?: string,
    page: number = 0,
    size: number = 50
  ): Observable<PageResponse<Task>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (projectId) params = params.set('projectId', projectId.toString());
    if (status) params = params.set('status', status);
    if (priority) params = params.set('priority', priority);
    if (assignedTo) params = params.set('assignedTo', assignedTo.toString());
    if (search) params = params.set('search', search);

    return this.http.get<PageResponse<Task>>(this.apiUrl, { params })
      .pipe(
        tap(response => this.tasksSubject.next(response.content))
      );
  }

  getTaskById(id: number): Observable<Task> {
    return this.http.get<Task>(`${this.apiUrl}/${id}`);
  }

  createTask(request: TaskRequest): Observable<Task> {
    return this.http.post<Task>(this.apiUrl, request)
      .pipe(
        tap(() => this.refreshTasks(request.projectId))
      );
  }

  updateTask(id: number, request: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}`, request)
      .pipe(
        tap(() => this.refreshTasks(request.projectId))
      );
  }

  updateTaskStatus(id: number, status: TaskStatus, projectId: number): Observable<Task> {
    const request: UpdateStatusRequest = { status };
    return this.http.patch<Task>(`${this.apiUrl}/${id}/status`, request)
      .pipe(
        tap(() => this.refreshTasks(projectId))
      );
  }

  deleteTask(id: number, projectId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`)
      .pipe(
        tap(() => this.refreshTasks(projectId))
      );
  }

  getTaskStats(projectId: number): Observable<TaskStats> {
    const params = new HttpParams().set('projectId', projectId.toString());
    return this.http.get<TaskStats>(`${this.apiUrl}/stats`, { params });
  }

  private refreshTasks(projectId: number): void {
    this.getAllTasks(projectId).subscribe();
  }
}
