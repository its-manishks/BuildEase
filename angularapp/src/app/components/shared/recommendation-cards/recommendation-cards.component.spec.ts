import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecommendationCardsComponent } from './recommendation-cards.component';

describe('RecommendationCardsComponent', () => {
  let component: RecommendationCardsComponent;
  let fixture: ComponentFixture<RecommendationCardsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RecommendationCardsComponent]
    });
    fixture = TestBed.createComponent(RecommendationCardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
