export interface UserSearchDto {
  id: string;
  username: string;
  displayName: string | null;
}

export interface ParticipantDto {
  userId: string;
  username: string;
  displayName: string | null;
  rsvp: 'PENDING' | 'ACCEPTED' | 'DECLINED';
}

export interface BrewEventResponse {
  id: string;
  title: string;
  description: string | null;
  brewDate: string;       // YYYY-MM-DD
  startTime: string | null; // HH:mm:ss
  location: string | null;
  recipeId: string | null;
  createdBy: string;
  createdAt: string;
  participants: ParticipantDto[];
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  brewDate: string;
  startTime?: string;
  location?: string;
  recipeId?: string;
  invitedUserIds?: string[];
}

export interface UpdateEventRequest {
  title: string;
  description?: string;
  brewDate: string;
  startTime?: string;
  location?: string;
  recipeId?: string;
}

export interface RsvpRequest {
  status: 'ACCEPTED' | 'DECLINED';
}
