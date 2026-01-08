import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MemoQuizSession } from './memo-quiz-session';

describe('MemoQuizSession', () => {
  let component: MemoQuizSession;
  let fixture: ComponentFixture<MemoQuizSession>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MemoQuizSession],
    }).compileComponents();

    fixture = TestBed.createComponent(MemoQuizSession);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
