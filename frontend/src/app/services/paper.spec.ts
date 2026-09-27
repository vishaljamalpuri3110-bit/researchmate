import { TestBed } from '@angular/core/testing';

import { Paper } from './paper';

describe('Paper', () => {
  let service: Paper;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Paper);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
