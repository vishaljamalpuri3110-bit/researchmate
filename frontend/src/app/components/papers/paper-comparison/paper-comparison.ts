
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import {
  PaperComparisonResponse,
  PaperService
} from '../../../services/paper.services';

@Component({
  selector: 'app-paper-comparison',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './paper-comparison.html',
  styleUrls: ['./paper-comparison.css']
})
export class PaperComparisonComponent implements OnInit {

  paperIds: number[] = [];

  comparison: PaperComparisonResponse | null = null;

  loading = true;
  errorMsg = '';

  constructor(
    private route: ActivatedRoute,
    private paperService: PaperService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {

    const idsParam = this.route.snapshot.queryParamMap.get('ids');

    if (!idsParam) {
      this.errorMsg = 'No papers selected for comparison.';
      this.loading = false;
      return;
    }

    this.paperIds = idsParam
      .split(',')
      .map(id => Number(id))
      .filter(id => !isNaN(id));

    if (this.paperIds.length < 2 || this.paperIds.length > 5) {
      this.errorMsg = 'Comparison requires between 2 and 5 papers.';
      this.loading = false;
      return;
    }

    this.loadComparison();
  }

  loadComparison(): void {

    this.loading = true;
    this.errorMsg = '';

    this.paperService
      .comparePapers(this.paperIds)
      .subscribe({

        next: (response) => {

          console.log('Comparison response:', response);

          this.comparison = response;
          this.loading = false;

          this.cdr.detectChanges();
        },

        error: (error) => {

          console.error('Comparison failed:', error);

          this.errorMsg =
            error?.error?.message ||
            'Unable to compare papers.';

          this.loading = false;

          this.cdr.detectChanges();
        }
      });
  }
}
