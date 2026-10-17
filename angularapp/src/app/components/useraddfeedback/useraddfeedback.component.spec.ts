import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UseraddfeedbackComponent } from './useraddfeedback.component';

describe('UseraddfeedbackComponent', () => {
  let component: UseraddfeedbackComponent;
  let fixture: ComponentFixture<UseraddfeedbackComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UseraddfeedbackComponent]
    });
    fixture = TestBed.createComponent(UseraddfeedbackComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
