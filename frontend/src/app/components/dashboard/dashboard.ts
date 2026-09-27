
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PaperService, Paper } from '../../services/paper.services';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {

  papers: Paper[] = [];

  loading = true;
  errorMsg = '';

  constructor(
    private paperService: PaperService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.errorMsg = '';

    this.paperService.getPapers().subscribe({
      next: (response) => {
        this.papers = response;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load dashboard:', error);

        this.errorMsg =
          error?.error?.message ||
          'Unable to load dashboard data.';

        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get totalPapers(): number {
    return this.papers.length;
  }

  get analyzedPapers(): number {
    return this.papers.filter(
      paper => paper.status === 'ANALYZED'
    ).length;
  }

  get pendingPapers(): number {
    return this.papers.filter(
      paper =>
        paper.status !== 'ANALYZED' &&
        paper.status !== 'FAILED'
    ).length;
  }

  get failedPapers(): number {
    return this.papers.filter(
      paper => paper.status === 'FAILED'
    ).length;
  }
}
