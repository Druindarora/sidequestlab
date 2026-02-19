import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './footer.html',
  styleUrls: ['./footer.scss'],
})
export class Footer {
  readonly currentYear = new Date().getFullYear();
  readonly contactEmail = 'contact@imaginecodebuild.dev';
  readonly githubUrl = 'https://github.com/Druindarora';
  readonly linkedInUrl = 'https://www.linkedin.com/in/st%C3%A9phane-boivin-94909997/';
}
