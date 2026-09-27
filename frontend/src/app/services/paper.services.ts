import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Paper {
  id: number;
  title: string;
  fileName?: string;
  status: string;
  publicationYear?: number;
  createdAt?: string;
}

export interface SimilarPaper {
  paperId: number;
  title: string;
  publicationYear?: number;
  method: string;
  score: number;
}
export interface PaperComparisonRequest {
  paperIds: number[];
}

export interface PaperComparisonItem {
  paperId: number;
  title: string;
  publicationYear?: number;
  researchProblem: string;
  methodology: string;
  dataset: string;
  algorithms: string[];
  results: string;
  limitations: string[];
  futureWork: string[];
}

export interface PaperComparisonResponse {
  papers: PaperComparisonItem[];
  comparativeSynthesis: string;
  generatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaperService {

  private readonly API_URL = 'http://localhost:8080/api/papers';

  constructor(private http: HttpClient) {}

  getPapers(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  getPaper(id: number): Observable<Paper> {
    return this.http.get<Paper>(`${this.API_URL}/${id}`);
  }

  uploadPaper(file: File, title: string,publicationYear: number): Observable<any> {
  const formData = new FormData();

  formData.append('file', file);
  formData.append('title', title);
  formData.append(
    'publicationYear',
    publicationYear.toString()
  );

  return this.http.post(
    `${this.API_URL}/upload`,
    formData
  );
}

  analyzePaper(id: number): Observable<any> {
    return this.http.post(
      `${this.API_URL}/${id}/analyze`,
      {}
    );
  }

  getAnalysis(id: number): Observable<any> {
    return this.http.get(
      `${this.API_URL}/${id}/analysis`
    );
  }

  getSimilar(
    id: number,
    method: 'TF_IDF' | 'EMBEDDING'
  ): Observable<SimilarPaper[]> {

    return this.http.get<SimilarPaper[]>(
      `${this.API_URL}/${id}/similar`,
      {
        params: { method }
      }
    );
  }

  comparePapers(paperIds: number[]): Observable<PaperComparisonResponse> {
  return this.http.post<PaperComparisonResponse>(
    `${this.API_URL}/compare`,
    { paperIds }
  );
}

  deletePaper(id: number): Observable<any> {
    return this.http.delete(
      `${this.API_URL}/${id}`
    );
  }
}