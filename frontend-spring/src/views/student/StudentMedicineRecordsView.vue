<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Student / Medicine Records"
        title="购药记录"
        description="真实读取 Agent 药房订单历史，支持筛选、查看明细和回查订单金额。"
      >
        <template #actions>
          <button class="button button--secondary" @click="loadOrders">刷新订单</button>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>

      <section class="glass-card panel stack">
        <div class="toolbar">
          <input v-model.trim="keyword" placeholder="搜索订单号或药品名称" />
          <select v-model="statusFilter">
            <option value="all">全部状态</option>
            <option value="completed">已完成</option>
            <option value="pending">处理中</option>
            <option value="cancelled">已取消</option>
          </select>
          <button class="button button--secondary" @click="resetFilters">重置筛选</button>
        </div>

        <table v-if="filteredOrders.length" class="data-table">
          <thead>
            <tr>
              <th>订单编号</th>
              <th>购买时间</th>
              <th>金额</th>
              <th>状态</th>
              <th>明细</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in filteredOrders" :key="order.id">
              <td>{{ order.order_no }}</td>
              <td>{{ formatDateTime(order.purchase_date) }}</td>
              <td>{{ formatMoney(order.total_amount) }}</td>
              <td><StatusBadge :label="statusLabel(order.status)" :tone="statusTone(order.status)" /></td>
              <td><button class="link-button" @click="openOrder(order)">查看详情</button></td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="loading" class="empty-state">订单数据加载中...</div>
        <div v-else class="empty-state">暂无购药订单。</div>
      </section>
    </div>

    <BaseModal :open="Boolean(selectedOrder)" title="订单详情" eyebrow="Pharmacy Order" size="sm" @close="selectedOrder = null">
      <div v-if="selectedOrder" class="stack">
        <div class="notice">
          <strong>{{ selectedOrder.order_no }}</strong>
          <p>{{ formatDateTime(selectedOrder.purchase_date) }} · {{ formatMoney(selectedOrder.total_amount) }}</p>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>药品</th>
              <th>规格</th>
              <th>数量</th>
              <th>价格</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in selectedOrder.items" :key="item.id">
              <td>{{ item.medicine_name }}</td>
              <td>{{ item.specification || '-' }}</td>
              <td>{{ item.quantity }} {{ item.unit || '' }}</td>
              <td>{{ formatMoney(item.price) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </BaseModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { pharmacyService, type PharmacyOrder } from '@/services/agent/pharmacy'
import { formatDateTime, formatMoney } from '@/utils/format'

const orders = ref<PharmacyOrder[]>([])
const selectedOrder = ref<PharmacyOrder | null>(null)
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<'all' | 'completed' | 'pending' | 'cancelled'>('all')
const errorMessage = ref('')

const filteredOrders = computed(() => orders.value.filter((order) => {
  const query = keyword.value.trim().toLowerCase()
  const matchesKeyword = !query
    || order.order_no.toLowerCase().includes(query)
    || order.items.some((item) => item.medicine_name.toLowerCase().includes(query))
  const matchesStatus = statusFilter.value === 'all' || order.status === statusFilter.value
  return matchesKeyword && matchesStatus
}))

const loadOrders = async (): Promise<void> => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await pharmacyService.listOrders({ page: 1, page_size: 100 })
    orders.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取订单失败'
  } finally {
    loading.value = false
  }
}

const statusLabel = (status: PharmacyOrder['status']): string => {
  if (status === 'completed') return '已完成'
  if (status === 'pending') return '处理中'
  return '已取消'
}

const statusTone = (status: PharmacyOrder['status']): 'success' | 'warning' | 'default' => {
  if (status === 'completed') return 'success'
  if (status === 'pending') return 'warning'
  return 'default'
}

const openOrder = (order: PharmacyOrder): void => {
  selectedOrder.value = order
}

const resetFilters = (): void => {
  keyword.value = ''
  statusFilter.value = 'all'
}

onMounted(() => {
  void loadOrders()
})
</script>
