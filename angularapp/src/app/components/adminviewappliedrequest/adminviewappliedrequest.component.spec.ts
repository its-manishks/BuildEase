import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminviewappliedrequestComponent } from './adminviewappliedrequest.component';

describe('AdminviewappliedrequestComponent', () => {
  let component: AdminviewappliedrequestComponent;
  let fixture: ComponentFixture<AdminviewappliedrequestComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AdminviewappliedrequestComponent]
    });
    fixture = TestBed.createComponent(AdminviewappliedrequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
