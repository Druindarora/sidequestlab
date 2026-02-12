import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { take } from 'rxjs/operators';
import { Header } from './core/layout/header/header';
import { Footer } from './core/layout/footer/footer';
import { AuthService } from './core/auth/auth.service';

export interface Profile {
  fullName: string;
  title: string;
  summary: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Header, Footer],
  templateUrl: './app.html',
  styleUrls: ['./app.scss'],
})
export class App implements OnInit {
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.authService.restoreSession().pipe(take(1)).subscribe();
  }
}
