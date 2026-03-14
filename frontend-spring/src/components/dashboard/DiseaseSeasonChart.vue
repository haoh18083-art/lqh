<template>
  <div ref="containerRef" class="chart-host"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'
import type { MonthlySeriesItem } from '@/services/http/adminDashboard'

const containerRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

const props = withDefaults(defineProps<{
  months?: string[]
  series?: MonthlySeriesItem[]
  currentMonth?: number
}>(), {
  months: () => [],
  series: () => [],
  currentMonth: 12
})

const palette = ['#b94732', '#4a7ab0', '#d37a52', '#275850', '#8e6dbf', '#6b8e23']

const getChartOption = (): EChartsOption => {
  if (!props.months.length || !props.series.length) {
    return {
      title: {
        text: '暂无疾病诊断数据',
        left: 'center',
        top: 'middle',
        textStyle: {
          color: '#6b7280',
          fontSize: 14,
          fontWeight: 500
        }
      }
    }
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: unknown): string => {
        const p = params as Array<{ axisValue: string; marker: string; seriesName: string; value: number }>
        let result = `<strong>${p[0].axisValue}</strong><br/>`
        p.forEach((param) => {
          const value = param.value
          if (value > 0) {
            result += `${param.marker} ${param.seriesName}: ${value} 例<br/>`
          } else {
            result += `${param.marker} ${param.seriesName}: <span style="color:#999">未统计</span><br/>`
          }
        })
        return result
      }
    },
    legend: {
      data: props.series.map((item) => item.name || '(空分类)'),
      bottom: 0,
      textStyle: { fontSize: 11 }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '18%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.months,
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: '#999',
          type: 'dashed'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '病例数',
      axisLabel: {
        formatter: '{value} 例'
      }
    },
    series: props.series.map((disease, index) => ({
      name: disease.name || '(空分类)',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: disease.data,
      itemStyle: { color: palette[index % palette.length] },
      lineStyle: { width: 2 },
      connectNulls: false,
      markLine: index === 0 ? {
        silent: true,
        symbol: 'none',
        data: [
          {
            xAxis: `${props.currentMonth}月`,
            lineStyle: {
              color: '#ff6b6b',
              type: 'dashed',
              width: 2
            },
            label: {
              formatter: '统计截止',
              position: 'end',
              color: '#ff6b6b'
            }
          }
        ]
      } : undefined
    }))
  }
}

const renderChart = (): void => {
  if (!containerRef.value) return
  chart ??= echarts.init(containerRef.value)
  chart.setOption(getChartOption())
}

const handleResize = (): void => {
  chart?.resize()
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

watch(() => [props.months, props.series, props.currentMonth], () => {
  renderChart()
}, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.chart-host {
  min-height: 350px;
  width: 100%;
}
</style>
