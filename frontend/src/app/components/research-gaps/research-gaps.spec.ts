import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResearchGaps } from './research-gaps';

describe('ResearchGaps', () => {
  let component: ResearchGaps;
  let fixture: ComponentFixture<ResearchGaps>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResearchGaps],
    }).compileComponents();

    fixture = TestBed.createComponent(ResearchGaps);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
