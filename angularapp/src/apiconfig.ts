import { environment } from './environments/environment';

/**
 * Base URL of the Spring Boot backend, read from src/environments/environment.ts
 * (git-ignored; copy environment.example.ts to create it).
 * Any trailing slash is removed, because a "//" in the URL is rejected by Spring Security.
 */
export const API_URL = environment.apiUrl.replace(/\/+$/, '');