import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Footer } from '../../../core/layout/footer/footer';
import { PrivateHeader } from '../private-header/private-header';

@Component({
  selector: 'app-private-layout',
  standalone: true,
  imports: [RouterOutlet, PrivateHeader, Footer],
  templateUrl: './private-layout.html',
  styleUrls: ['./private-layout.scss'],
})
export class PrivateLayout {}
