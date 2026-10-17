import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Chart, registerables } from 'chart.js';

import { AdminDashboard, ChartPoint } from '../../models/dashboard.model';
import { DashboardService } from '../../services/dashboard.service';

Chart.register(...registerables);

/**
 * Admin dashboard: KPI cards and animated charts. Every number comes from the database at the
 * moment the page loads (no sample or invented statistics). Charts with no data show a message
 * instead of an empty frame; animations are off for people who ask to reduce motion.
 */
@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit, OnDestroy {

  private static readonly STATUS_COLORS: Record<string, string> = {
    Pending: '#f59e0b', Approved: '#22c55e', Shipped: '#3b82f6', Rejected: '#ef4444'
  };

  @ViewChild('statusChart') statusCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryChart') categoryCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('trendChart') trendCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('ratingChart') ratingCanvas?: ElementRef<HTMLCanvasElement>;

  data: AdminDashboard | null = null;
  isLoading = false;
  errorMessage = '';

  private charts: Chart[] = [];
  private readonly reduceMotion =
    typeof window !== 'undefined' && !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

  constructor(private dashboardService: DashboardService) { }

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.dashboardService.getAdminDashboard().subscribe({
      next: (data: AdminDashboard) => {
        this.isLoading = false;
        this.data = data;
        // Wait one tick so the canvases inside *ngIf exist
        setTimeout(() => this.renderCharts());
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not load the dashboard.';
      }
    });
  }

  hasValues(points: ChartPoint[] | undefined): boolean {
    return !!points && points.some(point => point.value > 0);
  }

  get pendingShare(): number {
    const data = this.data;
    return data && data.totalRequests ? Math.round((data.pendingRequests / data.totalRequests) * 100) : 0;
  }

  private renderCharts(): void {
    const data = this.data;
    if (!data) {
      return;
    }
    this.destroyCharts();
    const animation = this.reduceMotion ? false : { duration: 900 };

    this.addChart(this.statusCanvas, {
      type: 'doughnut',
      data: {
        labels: data.requestsByStatus.map(point => point.label),
        datasets: [{
          data: data.requestsByStatus.map(point => point.value),
          backgroundColor: data.requestsByStatus.map(point => AdminDashboardComponent.STATUS_COLORS[point.label] ?? '#94a3b8'),
          borderWidth: 2
        }]
      },
      options: { animation, cutout: '62%', plugins: { legend: { position: 'bottom' } } }
    }, data.requestsByStatus);

    this.addChart(this.categoryCanvas, {
      type: 'bar',
      data: {
        labels: data.materialsByCategory.map(point => point.label),
        datasets: [{ label: 'Materials', data: data.materialsByCategory.map(point => point.value),
          backgroundColor: '#8b5cf6', borderRadius: 6 }]
      },
      options: { animation, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    }, data.materialsByCategory);

    this.addChart(this.trendCanvas, {
      type: 'line',
      data: {
        labels: data.requestsLast30Days.map(point => point.label),
        datasets: [{ label: 'Requests', data: data.requestsLast30Days.map(point => point.value),
          borderColor: '#2563eb', backgroundColor: 'rgba(37, 99, 235, 0.12)', fill: true, tension: 0.35, pointRadius: 2 }]
      },
      options: { animation, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    }, data.requestsLast30Days);

    this.addChart(this.ratingCanvas, {
      type: 'bar',
      data: {
        labels: data.ratingDistribution.map(point => point.label),
        datasets: [{ label: 'Reviews', data: data.ratingDistribution.map(point => point.value),
          backgroundColor: '#f5a623', borderRadius: 6 }]
      },
      options: { animation, indexAxis: 'y', plugins: { legend: { display: false } },
        scales: { x: { beginAtZero: true, ticks: { precision: 0 } } } }
    }, data.ratingDistribution);
  }

  private addChart(canvas: ElementRef<HTMLCanvasElement> | undefined, config: any, points: ChartPoint[]): void {
    if (canvas && this.hasValues(points)) {
      this.charts.push(new Chart(canvas.nativeElement, { ...config, options: { ...config.options, maintainAspectRatio: false } }));
    }
  }

  private destroyCharts(): void {
    this.charts.forEach(chart => chart.destroy());
    this.charts = [];
  }
}