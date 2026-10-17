import { TestBed } from '@angular/core/testing';

import { MaterialRequestService } from './material-request.service';

describe('MaterialRequestService', () => {
  let service: MaterialRequestService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MaterialRequestService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
