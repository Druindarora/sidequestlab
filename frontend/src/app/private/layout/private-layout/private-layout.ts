import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Footer } from '../../../core/layout/footer/footer';
import { Header } from '../../../core/layout/header/header';

@Component({
  selector: 'app-private-layout',
  standalone: true,
  imports: [RouterOutlet, Header, Footer],
  templateUrl: './private-layout.html',
  styleUrls: ['./private-layout.scss'],
})
export class PrivateLayout {}
