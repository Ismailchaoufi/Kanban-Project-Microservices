import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StatusFormDialogComponent } from './status-form-dialog.component';

describe('StatusFormDialogComponent', () => {
  let component: StatusFormDialogComponent;
  let fixture: ComponentFixture<StatusFormDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusFormDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StatusFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
