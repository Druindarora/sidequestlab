import { HttpClient } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

export interface Profile {
  fullName: string;
  title: string;
  summary: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  profile: Profile | undefined;
  protected readonly title = signal('Sidequestlab');

  private readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {
    // this.getData();
  }

  ngOnInit(): void {
    this.getData(); // Moved to constructor to ensure data is fetched on component initialization
  }

  getData(): void {
    this.getProfile().subscribe(data => {
      console.log(data); // ici tu vois déjà "William from database"
      this.profile = data;
    });
  }

  getProfile() {
    return this.http.get<Profile>(`${this.baseUrl}/profile/me`);
  }
}
