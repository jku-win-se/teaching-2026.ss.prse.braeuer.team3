import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RuleDto, RuleRequest } from './models';

/**
 * HTTP client service for IF-THEN rule management.
 * Covers US-012: zeitbasierte, schwellenwertbasierte und ereignisbasierte Trigger.
 * Covers US-014: Regelkonflikt-Erkennung.
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

  /**
   * Checks whether any existing enabled rules conflict with a proposed action (US-014).
   *
   * A conflict exists when another enabled rule targets the same action device with
   * the opposite action value (e.g. one turns a switch on, another turns it off).
   *
   * @param actionDeviceId  primary key of the device the new rule will control
   * @param actionValue     action the new rule will apply ("true", "false", "open", "close")
   * @param excludeRuleId   id of the rule being edited; omit when creating a new rule
   * @returns observable list of conflicting rules (empty = no conflicts)
   */
  checkConflicts(
    actionDeviceId: number,
    actionValue: string,
    excludeRuleId?: number,
  ): Observable<RuleDto[]> {
    let params = `actionDeviceId=${actionDeviceId}&actionValue=${encodeURIComponent(actionValue)}`;
    if (excludeRuleId != null) {
      params += `&excludeRuleId=${excludeRuleId}`;
    }
    return this.http.get<RuleDto[]>(`${this.BASE}/conflicts?${params}`);
  }
}
