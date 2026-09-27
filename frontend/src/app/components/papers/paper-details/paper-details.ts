import { ChangeDetectorRef,Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { PaperService, SimilarPaper } from '../../../services/paper.services';

@Component({
  selector: 'app-paper-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './paper-details.html',
  styleUrl: './paper-details.css'
})
export class PaperDetails implements OnInit {

  paperId!: number;

  paper: any = null;
  analysis: any = null;

  similarPapers: SimilarPaper[] = [];

  loading = true;
  analyzing = false;
  loadingAnalysis = false;
  loadingSimilar = false;

  errorMsg = '';
formatSimilarityScore(score: number): string {
  return `${Math.max(0, score * 100).toFixed(2)}%`;
}

  constructor(
    private route: ActivatedRoute,
    private paperService: PaperService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
  const id = this.route.snapshot.paramMap.get('id');

  console.log('Paper ID from URL:', id);

  this.paperId = Number(id);

  this.loadPaper();
  this.loadAnalysis();
}

loadPaper(): void {

  console.log('Loading paper:', this.paperId);

  this.paperService.getPaper(this.paperId).subscribe({

    next: (paper) => {

      console.log('Paper response:', paper);

      this.paper = paper;
      this.loading = false;

      this.cdr.detectChanges();
    },

    error: (error) => {

      console.error('Failed to load paper:', error);

      this.loading = false;

      this.errorMsg =
        error?.error?.message ||
        'Unable to load paper.';

      this.cdr.detectChanges();
    }

  });
}
  analyzePaper(): void {

    this.analyzing = true;
    this.errorMsg = '';

    this.paperService
      .analyzePaper(this.paperId)
      .subscribe({

        next: (response) => {
  console.log('Analysis response:', response);

  this.analyzing = false;

  this.loadAnalysis();
  this.loadPaper();

  this.cdr.detectChanges();
},

        error: (error) => {
  console.error('Analysis failed:', error);

  this.analyzing = false;

  this.errorMsg =
    error?.error?.message ||
    'Paper analysis failed.';

  this.cdr.detectChanges();
}
      });
  }

  loadAnalysis(): void {

    this.loadingAnalysis = true;

    this.paperService
      .getAnalysis(this.paperId)
      .subscribe({

        next: (analysis) => {
  console.log('Analysis:', analysis);

  this.analysis = analysis;
  this.loadingAnalysis = false;

  this.cdr.detectChanges();
},

        error: (error) => {
  console.log('No analysis available yet:', error);

  this.loadingAnalysis = false;

  this.cdr.detectChanges();
}
      });
  }

  loadSimilar(
    method: 'TF_IDF' | 'EMBEDDING'
  ): void {

    this.loadingSimilar = true;

    this.paperService
      .getSimilar(this.paperId, method)
      .subscribe({

        next: (papers) => {
  this.similarPapers = papers;
  this.loadingSimilar = false;

  this.cdr.detectChanges();
},
error: (error) => {
  console.error('Similar papers error:', error);

  this.loadingSimilar = false;

  this.errorMsg =
    error?.error?.message ||
    'Unable to load similar papers.';

  this.cdr.detectChanges();
}

      });
  }
}