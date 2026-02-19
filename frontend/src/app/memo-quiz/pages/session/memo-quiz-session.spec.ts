import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { MemoQuizSession } from './memo-quiz-session';
import { SessionControllerApi, SessionDto } from '../../../api';

describe('MemoQuizSession', () => {
  let component: MemoQuizSession;
  let fixture: ComponentFixture<MemoQuizSession>;
  let sessionApiMock: {
    todaySession: ReturnType<typeof vi.fn>;
    answer: ReturnType<typeof vi.fn>;
  };

  const todaySessionMock: SessionDto = {
    id: 42,
    startedAt: new Date('2026-02-11T00:00:00.000Z').toISOString(),
    cards: [{ cardId: 7, front: 'Question', back: 'Bonne reponse', box: 1 }],
  };

  beforeEach(async () => {
    sessionApiMock = {
      todaySession: vi.fn(() => of(todaySessionMock)),
      answer: vi.fn(() => of({ correct: true })),
    };

    await TestBed.configureTestingModule({
      imports: [MemoQuizSession],
      providers: [{ provide: SessionControllerApi, useValue: sessionApiMock as unknown as SessionControllerApi }],
    }).compileComponents();

    fixture = TestBed.createComponent(MemoQuizSession);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('sends the exact card answer when marking a response as correct', () => {
    component.phase = 'ANSWER';
    component.markAnswer(true);

    expect(sessionApiMock.answer).toHaveBeenCalledTimes(1);
    const payload = vi.mocked(sessionApiMock.answer).mock.calls[0][0];
    expect(payload.sessionId).toBe(42);
    expect(payload.cardId).toBe(7);
    expect(payload.answer).toBe('Bonne reponse');
  });

  it('sends an intentionally incorrect answer text when marking a response as incorrect', () => {
    component.phase = 'ANSWER';
    component.markAnswer(false);

    expect(sessionApiMock.answer).toHaveBeenCalledTimes(1);
    const payload = vi.mocked(sessionApiMock.answer).mock.calls[0][0];
    expect(payload.sessionId).toBe(42);
    expect(payload.cardId).toBe(7);
    expect(payload.answer).toContain('__mq_incorrect__');
    expect(payload.answer).not.toBe('Bonne reponse');
  });
});
