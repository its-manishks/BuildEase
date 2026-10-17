import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserviewappliedrequestComponent } from './userviewappliedrequest.component';

describe('UserviewappliedrequestComponent', () => {
  let component: UserviewappliedrequestComponent;
  let fixture: ComponentFixture<UserviewappliedrequestComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UserviewappliedrequestComponent]
    });
    fixture = TestBed.createComponent(UserviewappliedrequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
