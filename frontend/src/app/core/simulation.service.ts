import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SimulationRequest, SimulationResponse } from './models';

/**
 * HTTP client service for the day simulation feature (US-020).
 *
 * Sends start conditions and a day-of-week to the backend, which evaluates
 * all automation rules in-memory and returns the ordered event timeline.
 * The live system is never modified by the backend during a simulation run.
 */
@Injectable({ providedIn: 'root' })
export class SimulationService {
  private readonly BASE = 'http://localhost:8080/api/simulation';

  constructor(private http: HttpClient) {}

  /**
   * Runs a 24-hour day simulation.
   *
   * @param request simulation parameters (day of week + per-device start states)
   * @returns observable of the simulation response containing the event timeline
   */
  runSimulation(request: SimulationRequest): Observable<SimulationResponse> {
    return this.http.post<SimulationResponse>(`${this.BASE}/run`, request);
  }
}
