import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, catchError, debounceTime, distinctUntilChanged, filter, forkJoin, map, of, switchMap } from 'rxjs';

import { ReviewableMaterial } from '../../models/feedback.model';
import { Material } from '../../models/material.model';
import { FeedbackService } from '../../services/feedback.service';
import { ChatService } from '../../services/chat.service';
import { MaterialService } from '../../services/material.service';
import { SpellingIssue, WritingAssistService } from '../../services/writing-assist.service';

/**
 * "Write a review" for /user/feedback/add/:materialId (opened from a Shipped request).
 * Eligibility is checked up front (shipped and not reviewed yet); the backend enforces it again.
 * Final phase: spelling suggestions while typing and an optional "Improve wording" suggestion.
 * Both only SUGGEST (the user accepts or ignores) and the form works normally if they are unavailable.
 */
@Component({
  selector: 'app-useraddfeedback',
  templateUrl: './useraddfeedback.component.html',
  styleUrls: ['./useraddfeedback.component.css']
})
export class UseraddfeedbackComponent implements OnInit, OnDestroy {

  feedbackForm!: FormGroup;
  materialId: number | null = null;
  material: Material | null = null;
  eligible = false;
  isLoading = true;
  submitted = false;
  isSubmitting = false;
  showSuccessModal = false;
  errorMessage = '';

  /** Spelling suggestions for the current text (words the user ignored are hidden). */
  spellingIssues: SpellingIssue[] = [];
  aiEnabled = false;
  improving = false;
  improvedText = '';
  improveError = '';
  private readonly ignoredWords = new Set<string>();
  private spellSubscription?: Subscription;

  constructor(
    private formBuilder: FormBuilder,
    private feedbackService: FeedbackService,
    private materialService: MaterialService,
    private writingAssistService: WritingAssistService,
    private chatService: ChatService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.feedbackForm = this.formBuilder.group({
      rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
      feedbackText: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
    });
    const idParam = this.route.snapshot.paramMap.get('materialId');
    this.materialId = idParam ? Number(idParam) : null;
    this.load();
    this.watchSpelling();
    this.chatService.aiStatus().subscribe(status => this.aiEnabled = status.enabled);
  }

  ngOnDestroy(): void {
    this.spellSubscription?.unsubscribe();
  }

  /** Replaces only this occurrence, and only if the word is still there (the user may have edited it). */
  acceptSuggestion(issue: SpellingIssue, suggestion: string): void {
    const control = this.feedbackForm.get('feedbackText');
    const text: string = control?.value ?? '';
    let start = text.substring(issue.start, issue.end) === issue.word ? issue.start : -1;
    if (start < 0) {
      const match = new RegExp(`\\b${issue.word}\\b`).exec(text);
      start = match ? match.index : -1;
    }
    if (start >= 0) {
      control?.setValue(text.substring(0, start) + suggestion + text.substring(start + issue.word.length));
    }
    this.spellingIssues = this.spellingIssues.filter(i => i !== issue);
  }

  ignoreSuggestion(issue: SpellingIssue): void {
    this.ignoredWords.add(issue.word.toLowerCase());
    this.spellingIssues = this.spellingIssues.filter(i => i.word.toLowerCase() !== issue.word.toLowerCase());
  }

  improveWording(): void {
    const text: string = (this.feedbackForm.get('feedbackText')?.value ?? '').trim();
    if (text.length < 10) {
      this.improveError = 'Write at least 10 characters first.';
      return;
    }
    this.improving = true;
    this.improveError = '';
    this.writingAssistService.improve(text).subscribe({
      next: (response: { suggestion: string }) => {
        this.improving = false;
        this.improvedText = response.suggestion;
      },
      error: (err: any) => {
        this.improving = false;
        this.improveError = err.error?.message || 'Suggestions are not available right now.';
      }
    });
  }

  /** The user chose to replace their text with the suggestion. */
  useImprovedText(): void {
    this.feedbackForm.get('feedbackText')?.setValue(this.improvedText);
    this.improvedText = '';
  }

  /** Checks spelling 700 ms after typing stops; failures simply show no suggestions. */
  private watchSpelling(): void {
    const control = this.feedbackForm.get('feedbackText');
    if (!control) {
      return;
    }
    this.spellSubscription = control.valueChanges.pipe(
      debounceTime(700),
      map((value: string) => value ?? ''),
      distinctUntilChanged(),
      filter((value: string) => {
        if (value.trim().length < 3) {
          this.spellingIssues = [];
          return false;
        }
        return true;
      }),
      switchMap((value: string) => this.writingAssistService.spellcheck(value).pipe(
        map((response: { issues: SpellingIssue[] }) => response.issues),
        catchError(() => of([] as SpellingIssue[]))))
    ).subscribe((issues: SpellingIssue[]) => {
      this.spellingIssues = issues.filter(i => !this.ignoredWords.has(i.word.toLowerCase()));
    });
  }

  get rating(): number {
    return this.feedbackForm.get('rating')?.value ?? 0;
  }

  get textLength(): number {
    return (this.feedbackForm.get('feedbackText')?.value ?? '').length;
  }

  get imageSrc(): string | null {
    return this.materialService.imageSrc(this.material);
  }

  setRating(value: number): void {
    this.feedbackForm.patchValue({ rating: value });
  }

  showError(field: string, error: string): boolean {
    const control = this.feedbackForm.get(field);
    return !!control && (control.touched || this.submitted) && control.hasError(error);
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.feedbackForm.invalid || this.materialId === null || !this.eligible) {
      return;
    }
    const value = this.feedbackForm.value;
    this.isSubmitting = true;
    this.feedbackService.createReview({
      materialId: this.materialId,
      rating: Number(value.rating),
      feedbackText: value.feedbackText.trim()
    }).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showSuccessModal = true;
      },
      error: (err: any) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Could not submit your review. Please try again.';
      }
    });
  }

  onSuccessOk(): void {
    this.showSuccessModal = false;
    this.router.navigate(['/user/feedbacks']);
  }

  private load(): void {
    if (this.materialId === null) {
      this.isLoading = false;
      return;
    }
    forkJoin({
      material: this.materialService.getMaterialById(this.materialId).pipe(catchError(() => of(null as Material | null))),
      awaiting: this.feedbackService.getMaterialsAwaitingReview()
        .pipe(catchError(() => of([] as ReviewableMaterial[])))
    }).subscribe(({ material, awaiting }: { material: Material | null; awaiting: ReviewableMaterial[] }) => {
      this.isLoading = false;
      this.material = material;
      this.eligible = awaiting.some(item => item.materialId === this.materialId);
    });
  }
}