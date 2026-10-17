import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PreviousOrdersCarouselComponent } from './previous-orders-carousel.component';

describe('PreviousOrdersCarouselComponent', () => {
  let component: PreviousOrdersCarouselComponent;
  let fixture: ComponentFixture<PreviousOrdersCarouselComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PreviousOrdersCarouselComponent]
    });
    fixture = TestBed.createComponent(PreviousOrdersCarouselComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
