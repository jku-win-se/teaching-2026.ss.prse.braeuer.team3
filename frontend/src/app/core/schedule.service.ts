import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScheduleDto, ScheduleRequest } from './models';

/**
 * HTTP client service for schedule management.
 * Covers FR-09: Zeitpläne konfigurieren.
 */
@Injectable({ providedIn: 'root' })
export class ScheduleService {
  private readonly BASE = 'http://localhost:8080/api/schedules';

  constructor(private http: HttpClient) {}

  /**
   * Returns all schedules for the authenticated user.
   *
   * @returns observable list of schedules
   */
  getSchedules(): Observable<ScheduleDto[]> {
    return this.http.get<ScheduleDto[]>(this.BASE);
  }

  /**
   * Creates a new schedule.
   * FR-09: Zeitplan erstellen.
   *
   * @param req the schedule creation request
   * @returns observable of the newly created schedule
   */
  createSchedule(req: ScheduleRequest): Observable<ScheduleDto> {
    return this.http.post<ScheduleDto>(this.BASE, req);
  }

  /**
   * Updates an existing schedule.
   * FR-09: Zeitplan bearbeiten.
   *
   * @param id  the schedule's primary key
   * @param req the update request
   * @returns observable of the updated schedule
   */
  updateSchedule(id: number, req: ScheduleRequest): Observable<ScheduleDto> {
    return this.http.put<ScheduleDto>(`${this.BASE}/${id}`, req);
  }

  /**
   * Toggles the enabled flag of a schedule.
   * FR-09: Zeitplan aktivieren/deaktivieren.
   *
   * @param id      the schedule's primary key
   * @param enabled the new enabled state
   * @returns observable of the updated schedule
   */
  setEnabled(id: number, enabled: boolean): Observable<ScheduleDto> {
    return this.http.patch<ScheduleDto>(`${this.BASE}/${id}/enabled`, { enabled });
  }

  /**
   * Deletes a schedule.
   * FR-09: Zeitplan löschen.
   *
   * @param id the schedule's primary key
   * @returns observable that completes when the schedule is deleted
   */
  deleteSchedule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${id}`);
  }
}
