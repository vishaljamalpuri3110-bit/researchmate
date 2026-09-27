
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ResearchGapEvidence {
  paperId: number;
  paperTitle: string;
  evidenceText: string;
  sourceType: string;
}

export interface ResearchGap {
  id: number;
  title: string;
  description: string;
  confidence: number;
  frequency: number;
  supportingPapers: string[];
  evidence: ResearchGapEvidence[];
  disclaimer: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ResearchGapService {

  private readonly API_URL =
    'http://localhost:8080/api/research-gaps';

  constructor(private http: HttpClient) {}

  getResearchGaps(): Observable<ResearchGap[]> {
    return this.http.get<ResearchGap[]>(this.API_URL);
  }

  refreshResearchGaps(): Observable<ResearchGap[]> {
    return this.http.get<ResearchGap[]>(
      `${this.API_URL}?refresh=true`
    );
  }
}