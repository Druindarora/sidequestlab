import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { PrivateLayout } from './private-layout';

describe('PrivateLayout', () => {
  let fixture: ComponentFixture<PrivateLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrivateLayout],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(PrivateLayout);
    fixture.detectChanges();
  });

  it('should render the private shell', () => {
    expect(fixture.nativeElement.querySelector('app-private-header')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-public-header')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-footer')).toBeTruthy();
  });
});
