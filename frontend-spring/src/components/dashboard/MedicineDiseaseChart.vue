<template>
  <div ref="containerRef" class="chart-host"></div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'
import type { HeatmapPoint } from '@/services/http/adminDashboard'

const containerRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

const props = withDefaults(defineProps<{
  diseases?: string[]
  medicines?: string[]
  points?: HeatmapPoint[]
}>(), {
  diseases: () => [],
  medicines: () => [],
  points: () => []
})

const heatmapData = computed<[number, number, number][]>(() => props.points.map((point) => [
  point.medicine_index,
  point.disease_index,
  point.quantity
]))

const getChartOption = (): EChartsOption => {
  if (!props.diseases.length || !props.medicines.length) {
    return {
      title: {
        text: '暂无诊断处方数据',
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
      position: 'top',
      formatter: (params: unknown): string => {
        const p = params as { data: [number, number, number] }
        const medicine = props.medicines[p.data[0]] || '未命名药品'
        const disease = props.diseases[p.data[1]] || '(空分类)'
        const consumption = p.data[2]
        return `${disease}<br/>${medicine}: ${consumption} 盒`
      }
    },
    grid: {
      height: '70%',
      top: '10%',
      left: '15%',
      right: '10%'
    },
    xAxis: {
      type: 'category',
      data: props.medicines,
      splitArea: { show: true },
      axisLabel: {
        interval: 0,
        rotate: 30,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'category',
      data: props.diseases,
      splitArea: { show: true },
      axisLabel: {
        fontSize: 11
      }
    },
    visualMap: {
      min: 0,
      max: 150,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%',
      inRange: {
        color: ['#e8f5e9', '#81c784', '#388e3c', '#1b5e20']
      },
      text: ['高消耗', '低消耗']
    },
    series: [
      {
        name: '药品消耗',
        type: 'heatmap',
        data: heatmapData.value,
        label: {
          show: true,
          fontSize: 10,
          formatter: (params: unknown): string => {
            return String((params as { data: [number, number, number] }).data[2])
          }
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
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

watch(() => [props.diseases, props.medicines, props.points], () => {
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
  min-height: 400px;
  width: 100%;
}
</style>
