import { Component } from '@angular/core';
import { MATERIAL_IMPORTS } from '../../shared/material-imports';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-portfolio',
  imports: [MATERIAL_IMPORTS, RouterLink],
  templateUrl: './portfolio.html',
  styleUrls: ['./portfolio.scss'],
})
export class Portfolio {}
