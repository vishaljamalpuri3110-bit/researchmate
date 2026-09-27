import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import {
  Paper,
  PaperService
} from '../../../services/paper.services';

@Component({
  selector: 'app-paper-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './paper-list.html',
  styleUrls: ['./paper-list.css']
})
export class PaperListComponent implements OnInit {

  papers: Paper[] = [];

  selectedPaperIds: number[] = [];
  loading = true;
  errorMsg = '';

  constructor(
    private paperService: PaperService,
    private cdr: ChangeDetectorRef,
    private router:Router
  ) {}

  ngOnInit(): void {
    this.loadPapers();
  }

  loadPapers(): void {

    this.loading = true;

    this.paperService.getPapers().subscribe({

      next: (response) => {

        console.log('Papers response:', response);

        this.papers = response.content ?? response;
        this.loading = false;

        this.cdr.detectChanges();
      },

      error: (error) => {

        console.error('Failed to load papers:', error);

        this.errorMsg =
          error?.error?.message ||
          'Unable to load papers.';

        this.loading = false;

        this.cdr.detectChanges();
      }

    });
  }

  toggleSelection(id: number): void {

    if (this.selectedPaperIds.includes(id)) {

      this.selectedPaperIds =
        this.selectedPaperIds.filter(
          paperId => paperId !== id
        );

    } else {

      if (this.selectedPaperIds.length >= 5) {

        this.errorMsg =
          'You can compare a maximum of 5 papers.';

        return;
      }

      this.selectedPaperIds.push(id);
    }

    this.errorMsg = '';
  }

  isSelected(id: number): boolean {
    return this.selectedPaperIds.includes(id);
  }

  compareSelected(): void {

  if (this.selectedPaperIds.length < 2) {
    this.errorMsg = 'Select at least 2 papers to compare.';
    return;
  }

  if (this.selectedPaperIds.length > 5) {
    this.errorMsg = 'You can compare a maximum of 5 papers.';
    return;
  }

  this.router.navigate(
    ['/papers/compare'],
    {
      queryParams: {
        ids: this.selectedPaperIds.join(',')
      }
    }
  );
}
  deletePaper(id: number): void {

    if (!confirm('Delete this paper?')) {
      return;
    }

    this.paperService.deletePaper(id).subscribe({

      next: () => {

        this.papers =
          this.papers.filter(
            p => p.id !== id
          );

        this.selectedPaperIds =
          this.selectedPaperIds.filter(
            paperId => paperId !== id
          );

      },

      error: () => {

        this.errorMsg =
          'Unable to delete paper.';

      }

    });
  }
}