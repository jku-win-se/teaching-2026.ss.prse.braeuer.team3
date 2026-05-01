import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MemberResponseDto {
  id: number;
  name: string;
  email: string;
  joinedAt: string;
}

export interface MemberInviteRequest {
  email: string;
}

@Injectable({ providedIn: 'root' })
export class MemberService {
  private readonly BASE = 'http://localhost:8080/api/members';

  constructor(private http: HttpClient) {}

  getMembers(): Observable<MemberResponseDto[]> {
    return this.http.get<MemberResponseDto[]>(this.BASE);
  }

  inviteMember(email: string): Observable<MemberResponseDto> {
    const request: MemberInviteRequest = { email };
    return this.http.post<MemberResponseDto>(`${this.BASE}/invite`, request);
  }

  removeMember(memberId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${memberId}`);
  }
}
