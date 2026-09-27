import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaperDetails } from './paper-details';

describe('PaperDetails', () => {
  let component: PaperDetails;
  let fixture: ComponentFixture<PaperDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaperDetails],
    }).compileComponents();

    fixture = TestBed.createComponent(PaperDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
