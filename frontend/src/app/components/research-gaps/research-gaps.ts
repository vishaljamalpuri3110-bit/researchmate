
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ResearchGap,
  ResearchGapService
} from '../../services/research-gap.service';

@Component({
  selector: 'app-research-gaps',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './research-gaps.html',
  styleUrls: ['./research-gaps.css']
})
export class ResearchGapsComponent implements OnInit {

  researchGaps: ResearchGap[] = [];

  loading = true;
  refreshing = false;
  errorMsg = '';

  constructor(
    private researchGapService: ResearchGapService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadResearchGaps();
  }

  loadResearchGaps(): void {
    this.loading = true;
    this.errorMsg = '';

    this.researchGapService.getResearchGaps().subscribe({
      next: (response) => {
        this.researchGaps = response;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load research gaps:', error);

        this.errorMsg =
          error?.error?.message ||
          'Unable to load research gaps.';

        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  refreshResearchGaps(): void {
    this.refreshing = true;
    this.errorMsg = '';

    this.researchGapService.refreshResearchGaps().subscribe({
      next: (response) => {
        this.researchGaps = response;
        this.refreshing = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to refresh research gaps:', error);

        this.errorMsg =
          error?.error?.message ||
          'Unable to refresh research gaps.';

        this.refreshing = false;
        this.cdr.detectChanges();
      }
    });
  }
}
