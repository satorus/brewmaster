export interface UserDto {
  id: string;
  username: string;
  displayName: string | null;
  role: 'USER' | 'ADMIN';
}

export interface AuthResponse {
  token: string;
  expiresIn: number;
  user: UserDto;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}
