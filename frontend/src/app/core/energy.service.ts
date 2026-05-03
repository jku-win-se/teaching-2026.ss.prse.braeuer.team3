import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EnergyDevice } from './models';

/** HTTP client service for energy consumption estimates. */
@Injectable({ providedIn: 'root' })
export class EnergyService {
  private readonly API = 'http://localhost:8080/api/energy';

  constructor(private http: HttpClient) {}

  /** Returns estimated energy consumption for all devices in the household. */
  getDeviceEnergy(): Observable<EnergyDevice[]> {
    return this.http.get<EnergyDevice[]>(`${this.API}/devices`);
  }

  /**
   * Downloads the energy usage summary as a CSV file (FR-16).
   * Triggers a browser file-save dialog for "energy-summary.csv".
   */
  exportCsv(): void {
    this.http.get(`${this.API}/export`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'energy-summary.csv';
        anchor.click();
        URL.revokeObjectURL(url);
      },
    });
  }
}
