import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminInsightsComponent } from './admin-insights.component';

describe('AdminInsightsComponent', () => {
  let component: AdminInsightsComponent;
  let fixture: ComponentFixture<AdminInsightsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AdminInsightsComponent]
    });
    fixture = TestBed.createComponent(AdminInsightsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
