import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaperList } from './paper-list';

describe('PaperList', () => {
  let component: PaperList;
  let fixture: ComponentFixture<PaperList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaperList],
    }).compileComponents();

    fixture = TestBed.createComponent(PaperList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
