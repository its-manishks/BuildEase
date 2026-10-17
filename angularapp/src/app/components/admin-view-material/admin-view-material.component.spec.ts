import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminViewMaterialComponent } from './admin-view-material.component';

describe('AdminViewMaterialComponent', () => {
  let component: AdminViewMaterialComponent;
  let fixture: ComponentFixture<AdminViewMaterialComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AdminViewMaterialComponent]
    });
    fixture = TestBed.createComponent(AdminViewMaterialComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
