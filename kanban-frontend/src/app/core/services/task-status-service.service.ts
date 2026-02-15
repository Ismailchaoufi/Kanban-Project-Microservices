import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {TaskStatus, TaskStatusRequest} from '../models/task.model';

@Injectable({
  providedIn: 'root'
})
export class TaskStatusService {
  private apiUrl = `${environment.apiUrl}/projects`;

  constructor(private http: HttpClient) {}

  /**
   * Get all statuses for a project
   */
  getProjectStatuses(projectId: number): Observable<TaskStatus[]> {
    return this.http.get<TaskStatus[]>(
      `${this.apiUrl}/${projectId}/statuses`
    );
  }

  /**
   * Create a new custom status
   */
  createStatus(projectId: number, request: TaskStatusRequest): Observable<TaskStatus> {
    return this.http.post<TaskStatus>(
      `${this.apiUrl}/${projectId}/statuses`,
      request
    );
  }

  /**
   * Update a status (name, color)
   */
  updateStatus(
    projectId: number,
    statusId: number,
    request: TaskStatusRequest
  ): Observable<TaskStatus> {
    return this.http.put<TaskStatus>(
      `${this.apiUrl}/${projectId}/statuses/${statusId}`,
      request
    );
  }

  /**
   * Delete a status and move tasks to another status
   */
  deleteStatus(
    statusId: number,
    moveToStatusId: number,
    projectId: number
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${projectId}/statuses/${statusId}`,
      { params: { moveToStatusId: moveToStatusId.toString() } }
    );
  }

  /**
   * Reorder statuses (drag & drop columns)
   */
  reorderStatuses(projectId: number, statusIds: number[]): Observable<TaskStatus[]> {
    return this.http.put<TaskStatus[]>(
      `${this.apiUrl}/${projectId}/statuses/reorder`,
      { statusIds }
    );
  }
}
