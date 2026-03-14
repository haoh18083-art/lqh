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

const palette = ['#4a7ab0', '#d37a52', '#275850', '#b94732', '#8e6dbf', '#6b8e23', '#d14f8b', '#5b7c2b']

const getChartOption = (): EChartsOption => {
  if (!props.months.length || !props.series.length) {
    return {
      title: {
        text: '暂无药品消耗数据',
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
            result += `${param.marker} ${param.seriesName}: ${value} 盒<br/>`
          } else {
            result += `${param.marker} ${param.seriesName}: <span style="color:#999">未统计</span><br/>`
          }
        })
        return result
      }
    },
    legend: {
      data: props.series.map((item) => item.name || '未命名药品'),
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.months,
      axisLine: {
        lineStyle: {
          color: '#333'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '消耗量（盒）',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: props.series.map((item, index) => {
      const color = palette[index % palette.length]
      return {
        name: item.name || '未命名药品',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: item.data,
        itemStyle: { color },
        lineStyle: { width: 3, color },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `${color}40` },
            { offset: 1, color: `${color}05` }
          ])
        },
        markLine: index === 0 ? {
          silent: true,
          symbol: 'none',
          data: [
            {
              xAxis: `${props.currentMonth}月`,
              lineStyle: {
                color: '#999',
                type: 'dashed',
                width: 2
              },
              label: {
                formatter: '统计截止',
                position: 'end'
              }
            }
          ]
        } : undefined
      }
    })
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
