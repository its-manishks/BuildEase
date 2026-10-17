import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

/**
 * Small app-wide notifications (bottom-right). Use instead of alert():
 *   this.toast.success('Material saved');
 *   this.toast.error(err.error?.message || 'Could not save');
 * At most 4 are visible; each disappears on its own.
 */
@Injectable({
  providedIn: 'root'
})
export class ToastService {

  private static readonly MAX_VISIBLE = 4;

  private readonly toastsSubject = new BehaviorSubject<Toast[]>([]);
  readonly toasts$ = this.toastsSubject.asObservable();
  private nextId = 1;

  success(message: string): void {
    this.show('success', message, 4000);
  }

  info(message: string): void {
    this.show('info', message, 4000);
  }

  /** Errors stay a little longer so they can be read. */
  error(message: string): void {
    this.show('error', message, 6000);
  }

  dismiss(id: number): void {
    this.toastsSubject.next(this.toastsSubject.value.filter(toast => toast.id !== id));
  }

  private show(type: ToastType, message: string, durationMs: number): void {
    const toast: Toast = { id: this.nextId++, type, message };
    const visible = [...this.toastsSubject.value, toast].slice(-ToastService.MAX_VISIBLE);
    this.toastsSubject.next(visible);
    setTimeout(() => this.dismiss(toast.id), durationMs);
  }
}