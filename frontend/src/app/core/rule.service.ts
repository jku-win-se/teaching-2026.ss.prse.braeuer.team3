import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RuleDto, RuleRequest } from './models';

/**
 * HTTP client service for IF-THEN rule management.
 * Covers US-012: zeitbasierte, schwellenwertbasierte und ereignisbasierte Trigger.
 */
@Injectable({ providedIn: 'root' })
export class RuleService {
  private readonly BASE = 'http://localhost:8080/api/rules';

  constructor(private http: HttpClient) {}

  /**
   * Returns all rules for the authenticated user.
   *
   * @returns observable list of rules
   */
  getRules(): Observable<RuleDto[]> {
    return this.http.get<RuleDto[]>(this.BASE);
  }

  /**
   * Creates a new IF-THEN rule.
   *
   * @param req the rule creation request
   * @returns observable of the newly created rule
   */
  createRule(req: RuleRequest): Observable<RuleDto> {
    return this.http.post<RuleDto>(this.BASE, req);
  }

  /**
   * Fully replaces an existing rule.
   *
   * @param id  the rule's primary key
   * @param req the replacement request
   * @returns observable of the updated rule
   */
  updateRule(id: number, req: RuleRequest): Observable<RuleDto> {
    return this.http.put<RuleDto>(`${this.BASE}/${id}`, req);
  }

  /**
   * Toggles the enabled flag of a rule.
   *
   * @param id      the rule's primary key
   * @param enabled the new enabled state
   * @returns observable of the updated rule
   */
  setEnabled(id: number, enabled: boolean): Observable<RuleDto> {
    return this.http.patch<RuleDto>(`${this.BASE}/${id}/enabled`, { enabled });
  }

  /**
   * Deletes a rule.
   *
   * @param id the rule's primary key
   * @returns observable that completes when deletion is done
   */
  deleteRule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${id}`);
  }
}
