import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaperUploadComponent } from './paper-upload';

describe('PaperUpload', () => {
  let component: PaperUploadComponent;
  let fixture: ComponentFixture<PaperUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaperUploadComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PaperUploadComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
