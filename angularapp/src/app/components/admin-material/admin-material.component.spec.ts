import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminMaterialComponent } from './admin-material.component';

describe('AdminMaterialComponent', () => {
  let component: AdminMaterialComponent;
  let fixture: ComponentFixture<AdminMaterialComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AdminMaterialComponent]
    });
    fixture = TestBed.createComponent(AdminMaterialComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
