import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UseraddrequestComponent } from './useraddrequest.component';

describe('UseraddrequestComponent', () => {
  let component: UseraddrequestComponent;
  let fixture: ComponentFixture<UseraddrequestComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UseraddrequestComponent]
    });
    fixture = TestBed.createComponent(UseraddrequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
