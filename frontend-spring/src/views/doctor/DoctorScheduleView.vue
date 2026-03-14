<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Doctor / Schedule"
        title="我的排班"
        description="真实读取医生排班接口，按日期展示时段、容量与已预约人数。"
      >
        <template #actions>
          <button class="button button--secondary" @click="loadSchedules">刷新排班</button>
        </template>
      </PageHero>

      <section class="glass-card panel stack">
        <div class="toolbar">
          <input v-model="dateFrom" type="date" />
          <input v-model="dateTo" type="date" />
          <button class="button" @click="loadSchedules">查询排班</button>
        </div>

        <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>

        <div v-if="loading" class="empty-state">排班数据加载中...</div>
        <div v-else-if="groupedSchedules.length" class="schedule-grid">
          <article v-for="group in groupedSchedules" :key="group.date" class="glass-card schedule-day">
            <div class="schedule-day__head">
              <div>
                <h3>{{ group.date }}</h3>
                <p>{{ group.items.length }} 个时段</p>
              </div>
            </div>
            <ul class="schedule-day__list">
              <li v-for="item in group.items" :key="item.id">
                <strong>{{ item.time_slot }}</strong>
                <span>容量 {{ item.capacity }}</span>
                <span>已约 {{ item.booked_count }}</span>
                <StatusBadge :label="item.status === 'open' ? '开放' : '关闭'" :tone="item.status === 'open' ? 'success' : 'default'" />
              </li>
            </ul>
          </article>
        </div>
        <div v-else class="empty-state">当前时间范围内没有排班。</div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { scheduleService } from '@/services/http/schedule'
import type { DoctorScheduleItem } from '@/types/schedule'

const loading = ref(false)
const errorMessage = ref('')
const schedules = ref<DoctorScheduleItem[]>([])
const dateFrom = ref(new Date().toISOString().slice(0, 10))
const dateTo = ref(new Date(Date.now() + (13 * 24 * 60 * 60 * 1000)).toISOString().slice(0, 10))

const groupedSchedules = computed(() => {
  const map = new Map<string, DoctorScheduleItem[]>()
  for (const item of schedules.value) {
    const bucket = map.get(item.date) || []
    bucket.push(item)
    map.set(item.date, bucket)
  }

  return Array.from(map.entries()).map(([date, items]) => ({
    date,
    items: items.sort((left, right) => left.time_slot.localeCompare(right.time_slot))
  }))
})

const loadSchedules = async (): Promise<void> => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await scheduleService.mine({
      date_from: dateFrom.value,
      date_to: dateTo.value
    })
    schedules.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取排班失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadSchedules()
})
</script>

<style scoped>
.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.schedule-day {
  padding: 20px;
}

.schedule-day__head p {
  margin: 8px 0 0;
  color: var(--muted);
}

.schedule-day__list {
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
  display: grid;
  gap: 12px;
}

.schedule-day__list li {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(24, 49, 45, 0.05);
}
</style>
