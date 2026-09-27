import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PaperService } from '../../../services/paper.services';
import { FormsModule } from '@angular/forms';
@Component({
  selector: 'app-paper-upload',
  standalone: true,
  imports: [CommonModule,FormsModule],
  templateUrl: './paper-upload.html',
  styleUrls: ['./paper-upload.css']
})
export class PaperUploadComponent {

  selectedFile: File | null = null;
  uploading = false;
  errorMsg = '';
  title = '';
  publicationYear: number | null = null;
  constructor(
    private paperService: PaperService,
    private router: Router
  ) {}

  onFileSelected(event: Event): void {

    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    if (file.type !== 'application/pdf') {
      this.errorMsg = 'Only PDF files are allowed.';
      this.selectedFile = null;
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.errorMsg = 'PDF must be smaller than 10 MB.';
      this.selectedFile = null;
      return;
    }

    this.errorMsg = '';
    this.selectedFile = file;
  }

  upload(): void {

    if (!this.title.trim()) {
  this.errorMsg = 'Please enter a paper title.';
  return;
}

if (!this.publicationYear) {
  this.errorMsg = 'Please enter the publication year.';
  return;
}

    if (!this.selectedFile) {
      this.errorMsg = 'Please select a PDF file.';
      return;
    }

    this.uploading = true;
    this.errorMsg = '';
    
    this.paperService
      .uploadPaper(this.selectedFile, this.title,this.publicationYear)
      .subscribe({

        next: (paper) => {
          this.uploading = false;

          this.router.navigate([
            '/papers',
            paper.id
          ]);
        },

        error: (error) => {
          this.uploading = false;

          this.errorMsg =
            error?.error?.message ||
            'Upload failed. Please try again.';
        }

      });
  }
}