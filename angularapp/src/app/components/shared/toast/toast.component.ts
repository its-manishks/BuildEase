import { Component } from '@angular/core';

import { Toast, ToastService } from '../../../services/toast.service';

/** Renders ToastService messages. Hosted once in app.component.html. */
@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css']
})
export class ToastComponent {

  readonly toasts$ = this.toastService.toasts$;

  constructor(private toastService: ToastService) { }

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }

  trackById(_index: number, toast: Toast): number {
    return toast.id;
  }
}