import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActivityLogDto, SpringPage } from './models';

const BASE = 'http://localhost:8080/api/activity-log';

/**
 * HTTP client service for the activity log (FR-08).
 *
 * Provides methods to fetch a paginated, filtered list of activity log entries
 * and to delete individual entries.
 */
@Injectable({ providedIn: 'root' })
export class ActivityLogService {

  constructor(private http: HttpClient) {}

  /**
   * Returns a paginated page of activity log entries for the authenticated user.
   *
   * @param page     zero-based page index
   * @param size     number of entries per page
   * @param from     optional ISO-8601 start timestamp filter
   * @param to       optional ISO-8601 end timestamp filter
   * @param deviceId optional device id filter
   */
  getLogs(
    page = 0,
    size = 20,
    from?: string,
    to?: string,
    deviceId?: number
  ): Observable<SpringPage<ActivityLogDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (from) { params = params.set('from', from); }
    if (to) { params = params.set('to', to); }
    if (deviceId !== undefined && deviceId !== null) {
      params = params.set('deviceId', deviceId.toString());
    }
    return this.http.get<SpringPage<ActivityLogDto>>(BASE, { params });
  }

  /**
   * Deletes a single activity log entry.
   *
   * @param id the log entry primary key
   */
  deleteLog(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  /**
   * Downloads the complete activity log as a CSV file (FR-16).
   * Triggers a browser file-save dialog for "activity-log.csv".
   */
  exportCsv(): void {
    this.http.get(`${BASE}/export`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'activity-log.csv';
        anchor.click();
        URL.revokeObjectURL(url);
      },
    });
  }
}
