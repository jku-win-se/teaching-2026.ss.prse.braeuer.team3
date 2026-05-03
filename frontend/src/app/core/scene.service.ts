import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SceneDto, SceneCreateRequest } from './models';

/**
 * HTTP client service for scene management.
 * Covers US-018: Szenen erstellen, aktivieren, bearbeiten und löschen.
 */
@Injectable({ providedIn: 'root' })
export class SceneService {
  private readonly BASE = 'http://localhost:8080/api/scenes';

  constructor(private http: HttpClient) {}

  /**
   * Returns all scenes owned by the authenticated user.
   *
   * @returns observable list of scenes
   */
  getScenes(): Observable<SceneDto[]> {
    return this.http.get<SceneDto[]>(this.BASE);
  }

  /**
   * Creates a new scene.
   *
   * @param req the scene creation request
   * @returns observable of the newly created scene
   */
  createScene(req: SceneCreateRequest): Observable<SceneDto> {
    return this.http.post<SceneDto>(this.BASE, req);
  }

  /**
   * Fully replaces an existing scene.
   *
   * @param id  the scene's primary key
   * @param req the replacement request
   * @returns observable of the updated scene
   */
  updateScene(id: number, req: SceneCreateRequest): Observable<SceneDto> {
    return this.http.put<SceneDto>(`${this.BASE}/${id}`, req);
  }

  /**
   * Deletes a scene.
   *
   * @param id the scene's primary key
   * @returns observable that completes when deletion is done
   */
  deleteScene(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${id}`);
  }

  /**
   * Activates a scene, applying all its device-action entries.
   *
   * @param id the scene's primary key
   * @returns observable that completes when activation is done
   */
  activateScene(id: number): Observable<void> {
    return this.http.post<void>(`${this.BASE}/${id}/activate`, {});
  }
}
