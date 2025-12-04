import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  imports: [],
  templateUrl: './footer.html',
  styleUrl: './footer.scss',
})
export class Footer {
  // footer.ts (ex. standalone)
  currentYear = new Date().getFullYear();
}
