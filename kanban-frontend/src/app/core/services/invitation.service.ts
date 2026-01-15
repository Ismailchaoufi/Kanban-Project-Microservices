import { Injectable } from '@angular/core';
import {environment} from '../../../environments/environment';
import {Observable} from 'rxjs';
import {HttpClient, HttpParams} from '@angular/common/http';

export interface InvitationInfoResponse {
  email: string;
  projectName: string;
  invitedBy: string;
  expired: boolean;
  valid: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class InvitationService {

  private apiUrl = `${environment.apiUrl}/projects`;
  constructor(private http: HttpClient) {}

  /**
   * Vérifier un token d'invitation
   */
  verifyInvitation(token: string): Observable<InvitationInfoResponse> {
    const params = new HttpParams().set('token', token);
    return this.http.get<InvitationInfoResponse>(
      `${this.apiUrl}/invitations/verify`,
      { params }
    );
  }

  /**
   * Accepter une invitation
   */
  acceptInvitation(token: string): Observable<string> {
    const params = new HttpParams().set('token', token);
    return this.http.post(
      `${this.apiUrl}/invitations/accept`,
      null,
      {
        params,
        responseType: 'text'
      }
    );
  }
}
