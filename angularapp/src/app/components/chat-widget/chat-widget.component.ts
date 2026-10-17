import { AfterViewChecked, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Subscription, distinctUntilChanged, skip } from 'rxjs';

import { ChatReply, ChatTurn } from '../../models/chat.model';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';

interface ChatMessage {
  from: 'user' | 'bot';
  text: string;
  error?: boolean;
}

/**
 * BuildEase assistant launcher and chat panel, available on every page including login.
 * Closed and silent until the visitor clicks it: no greeting, badge or popup.
 * The conversation lives only in this component and is cleared when the account changes
 * (login, logout), so the next person on a shared computer never sees it.
 */
@Component({
  selector: 'app-chat-widget',
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.css']
})
export class ChatWidgetComponent implements OnInit, OnDestroy, AfterViewChecked {

  @ViewChild('messageInput') messageInput?: ElementRef<HTMLInputElement>;
  @ViewChild('launcher') launcher?: ElementRef<HTMLButtonElement>;
  @ViewChild('scrollArea') scrollArea?: ElementRef<HTMLDivElement>;

  readonly maxLength = 500;
  open = false;
  messages: ChatMessage[] = [];
  draft = '';
  sending = false;

  private scrollPending = false;
  private subscription?: Subscription;

  constructor(private chatService: ChatService, private authService: AuthService) { }

  ngOnInit(): void {
    // A different account (or logging out) starts a fresh, empty conversation
    this.subscription = this.authService.userId$.pipe(distinctUntilChanged(), skip(1)).subscribe(() => {
      this.messages = [];
      this.draft = '';
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  ngAfterViewChecked(): void {
    if (this.scrollPending && this.scrollArea) {
      this.scrollArea.nativeElement.scrollTop = this.scrollArea.nativeElement.scrollHeight;
      this.scrollPending = false;
    }
  }

  toggle(): void {
    this.open ? this.close() : this.openPanel();
  }

  close(): void {
    this.open = false;
    setTimeout(() => this.launcher?.nativeElement.focus());
  }

  send(): void {
    const text = this.draft.trim();
    if (!text || this.sending) {
      return;
    }
    const history: ChatTurn[] = this.messages.filter(m => !m.error)
      .map(m => ({ role: m.from === 'user' ? 'user' : 'model', text: m.text } as ChatTurn));
    this.messages.push({ from: 'user', text });
    this.draft = '';
    this.sending = true;
    this.scrollPending = true;
    this.chatService.send(text, history).subscribe({
      next: (reply: ChatReply) => this.addBotMessage(reply.reply),
      error: (err: any) => this.addBotMessage(this.errorText(err), true)
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.close();
    }
  }

  private openPanel(): void {
    this.open = true;
    this.scrollPending = true;
    setTimeout(() => this.messageInput?.nativeElement.focus());
  }

  private addBotMessage(text: string, error = false): void {
    this.sending = false;
    this.messages.push({ from: 'bot', text, error });
    this.scrollPending = true;
  }

  private errorText(err: any): string {
    if (err.status === 429 || err.status === 400) {
      return err.error?.message || 'Please wait a moment and try again.';
    }
    if (err.error?.errorCode === 'PASSWORD_CHANGE_REQUIRED') {
      return 'Please finish setting your new password first.';
    }
    return 'The assistant is unavailable right now. Please try again later.';
  }
}