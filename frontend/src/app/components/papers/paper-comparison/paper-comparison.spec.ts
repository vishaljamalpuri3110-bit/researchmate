import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaperComparison } from './paper-comparison';

describe('PaperComparison', () => {
  let component: PaperComparison;
  let fixture: ComponentFixture<PaperComparison>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaperComparison],
    }).compileComponents();

    fixture = TestBed.createComponent(PaperComparison);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
