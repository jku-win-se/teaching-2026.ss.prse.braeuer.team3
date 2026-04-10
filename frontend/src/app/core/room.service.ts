import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RoomDto {
  id: number;
  name: string;
  icon: string;
}

export interface CreateRoomRequest {
  name: string;
  icon: string;
}

export interface RenameRoomRequest {
  name: string;
}

/**
 * Service for managing rooms via the SmartHome backend REST API.
 * Covers US-004: create, rename and delete rooms.
 */
@Injectable({ providedIn: 'root' })
export class RoomService {
  private readonly API = 'http://localhost:8080/api/rooms';

  constructor(private http: HttpClient) {}

  /**
   * Retrieves all rooms belonging to the authenticated user.
   *
   * @returns observable list of rooms
   */
  getRooms(): Observable<RoomDto[]> {
    return this.http.get<RoomDto[]>(this.API);
  }

  /**
   * Creates a new room with the given name and icon.
   * US-004: Raum mit Name erstellen möglich
   *
   * @param req the room creation request containing name and icon
   * @returns observable of the newly created room
   */
  createRoom(req: CreateRoomRequest): Observable<RoomDto> {
    return this.http.post<RoomDto>(this.API, req);
  }

  /**
   * Renames an existing room.
   * US-004: Raum umbenennen möglich
   *
   * @param id   the room identifier
   * @param req  the rename request containing the new name
   * @returns observable of the updated room
   */
  renameRoom(id: number, req: RenameRoomRequest): Observable<RoomDto> {
    return this.http.put<RoomDto>(`${this.API}/${id}`, req);
  }

  /**
   * Deletes a room by its identifier.
   * US-004: Raum löschen möglich (inkl. Hinweis bei vorhandenen Geräten)
   * The backend returns 409 Conflict when the room still has devices.
   *
   * @param id the room identifier
   * @returns observable that completes when deletion is done
   */
  deleteRoom(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }
}
