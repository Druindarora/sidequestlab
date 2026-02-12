import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { MemoQuizHome } from './memo-quiz-home';
import { DashboardControllerApi } from '../../../api';

describe('MemoQuizHome', () => {
  let component: MemoQuizHome;
  let fixture: ComponentFixture<MemoQuizHome>;
  let dashboardApiMock: {
    today: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    dashboardApiMock = {
      today: vi.fn(() =>
        of({
          todayDate: '2026-02-12',
          dayIndex: 10,
          canStartSession: true,
          boxesToday: [1, 3],
          dueToday: 6,
          totalCards: 25,
          lastSessionSummary: null,
          boxesOverview: [{ boxNumber: 1, cardCount: 10, isToday: true }],
        }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [MemoQuizHome],
      providers: [
        provideRouter([]),
        { provide: DashboardControllerApi, useValue: dashboardApiMock as unknown as DashboardControllerApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MemoQuizHome);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
