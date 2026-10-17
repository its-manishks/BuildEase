import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserviewfeedbackComponent } from './userviewfeedback.component';

describe('UserviewfeedbackComponent', () => {
  let component: UserviewfeedbackComponent;
  let fixture: ComponentFixture<UserviewfeedbackComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UserviewfeedbackComponent]
    });
    fixture = TestBed.createComponent(UserviewfeedbackComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
