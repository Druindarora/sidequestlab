import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MemoQuizHome } from './memo-quiz-home';

describe('MemoQuizHome', () => {
  let component: MemoQuizHome;
  let fixture: ComponentFixture<MemoQuizHome>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MemoQuizHome]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MemoQuizHome);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
