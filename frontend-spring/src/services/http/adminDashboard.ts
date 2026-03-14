import { request } from './apiClient'

export interface HeatmapPoint {
  disease_index: number
  medicine_index: number
  quantity: number
}

export interface MedicineDiseaseHeatmapResponse {
  diseases: string[]
  medicines: string[]
  points: HeatmapPoint[]
}

export interface MonthlySeriesItem {
  name: string
  data: number[]
}

export interface MonthlySeriesResponse {
  months: string[]
  series: MonthlySeriesItem[]
}

export interface AdminOperationChartsResponse {
  medicine_disease_heatmap: MedicineDiseaseHeatmapResponse
  medicine_season_series: MonthlySeriesResponse
  disease_season_series: MonthlySeriesResponse
  meta: {
    current_year: number
    current_month: number
    generated_at: string
  }
}

export const adminDashboardService = {
  getOperationCharts(): Promise<AdminOperationChartsResponse> {
    return request.get<AdminOperationChartsResponse>('/admin/dashboard/operation-charts').then((response) => {
      if (!response.data) {
        throw new Error('获取运营统计图表失败')
      }
      return response.data
    })
  }
}
