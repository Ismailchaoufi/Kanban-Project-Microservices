import { TestBed } from '@angular/core/testing';

import { TaskStatusServiceService } from './task-status-service.service';

describe('TaskStatusServiceService', () => {
  let service: TaskStatusServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TaskStatusServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
