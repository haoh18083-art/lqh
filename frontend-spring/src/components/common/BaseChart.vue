<template>
  <div ref="containerRef" class="chart-host"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'

const props = defineProps<{
  option: Record<string, unknown>
}>()

const containerRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

const renderChart = (): void => {
  if (!containerRef.value) {
    return
  }

  chart ??= echarts.init(containerRef.value)
  chart.setOption(props.option as EChartsOption)
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', renderChart)
})

watch(() => props.option, renderChart, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderChart)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.chart-host {
  min-height: 320px;
  width: 100%;
}
</style>
