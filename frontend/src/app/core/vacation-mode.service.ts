import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VacationModeDto, VacationModeRequest } from './models';

/**
 * HTTP client service for vacation mode management.
 * Covers FR-21: Urlaubsmodus.
 */
@Injectable({ providedIn: 'root' })
export class VacationModeService {
  private readonly BASE = 'http://localhost:8080/api/vacation-modes';

  constructor(private http: HttpClient) {}

  /**
   * Returns all vacation modes for the authenticated owner.
   *
   * @returns observable list of vacation modes
   */
  getVacationModes(): Observable<VacationModeDto[]> {
    return this.http.get<VacationModeDto[]>(this.BASE);
  }

  /**
   * Creates a new vacation mode.
   * FR-21: Urlaubsmodus konfigurieren.
   *
   * @param req the vacation mode creation request
   * @returns observable of the newly created vacation mode
   */
  createVacationMode(req: VacationModeRequest): Observable<VacationModeDto> {
    return this.http.post<VacationModeDto>(this.BASE, req);
  }

  /**
   * Permanently deactivates a vacation mode.
   * FR-21: Urlaubsmodus vorzeitig deaktivieren.
   *
   * @param id the vacation mode's primary key
   * @returns observable of the updated vacation mode
   */
  deactivate(id: number): Observable<VacationModeDto> {
    return this.http.patch<VacationModeDto>(`${this.BASE}/${id}/deactivate`, {});
  }

  /**
   * Deletes a vacation mode.
   * FR-21: Urlaubsmodus löschen.
   *
   * @param id the vacation mode's primary key
   * @returns observable that completes when deleted
   */
  deleteVacationMode(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${id}`);
  }
}
