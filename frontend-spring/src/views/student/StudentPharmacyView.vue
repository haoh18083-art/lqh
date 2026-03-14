<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Student / Pharmacy"
        title="自助药房"
        description="完整保留 Agent 驱动的自助购药流程：检索药品、加入购物车、统一结算并回写订单。"
      >
        <template #actions>
          <button class="button" @click="loadMedicines">刷新药品</button>
          <button class="button button--secondary" @click="drawerOpen = true">购物车 ({{ cartCount }})</button>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <section class="glass-card panel stack">
        <div class="toolbar">
          <input v-model.trim="searchText" placeholder="搜索药品名称" />
          <select v-model="categoryFilter">
            <option value="">全部分类</option>
            <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
          </select>
          <button class="button button--secondary" @click="resetFilters">重置筛选</button>
        </div>

        <table v-if="filteredMedicines.length" class="data-table">
          <thead>
            <tr>
              <th>药品名称</th>
              <th>分类</th>
              <th>规格</th>
              <th>价格</th>
              <th>库存</th>
              <th>状态</th>
              <th>数量</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="medicine in filteredMedicines" :key="medicine.id">
              <td>
                <strong>{{ medicine.name }}</strong>
              </td>
              <td>{{ medicine.category }}</td>
              <td>{{ medicine.specification || '-' }}</td>
              <td>{{ formatMoney(medicine.price) }}</td>
              <td>{{ medicine.stock }} {{ medicine.unit || '' }}</td>
              <td><StatusBadge :label="stockLabel(medicine)" :tone="stockTone(medicine)" /></td>
              <td>
                <div class="quantity-stepper">
                  <button class="button button--secondary" :disabled="!cartMap[medicine.id]" @click="decreaseQuantity(medicine.id)">-</button>
                  <input
                    :value="cartMap[medicine.id]?.quantity || 0"
                    type="number"
                    min="0"
                    :max="medicine.stock"
                    @input="updateQuantity(medicine.id, Number(($event.target as HTMLInputElement).value || 0))"
                  />
                  <button class="button" :disabled="medicine.stock <= (cartMap[medicine.id]?.quantity || 0)" @click="increaseQuantity(medicine.id)">+</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="loading" class="empty-state">药品数据加载中...</div>
        <div v-else class="empty-state">没有符合条件的药品。</div>
      </section>
    </div>

    <BaseModal :open="drawerOpen" title="购物车" eyebrow="Pharmacy Cart" size="md" @close="drawerOpen = false">
      <div class="stack">
        <table v-if="cartItems.length" class="data-table">
          <thead>
            <tr>
              <th>药品</th>
              <th>单价</th>
              <th>数量</th>
              <th>小计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in cartItems" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ formatMoney(item.price) }}</td>
              <td>{{ item.quantity }}</td>
              <td>{{ formatMoney(item.price * item.quantity) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">购物车为空。</div>

        <div class="notice">
          共 {{ cartCount }} 件药品，总金额 <strong>{{ formatMoney(cartTotal) }}</strong>
        </div>

        <div class="toolbar">
          <button class="button button--secondary" @click="clearCart">清空购物车</button>
          <button class="button" :disabled="checkoutLoading || !cartItems.length" @click="checkout">
            {{ checkoutLoading ? '结算中...' : '提交订单' }}
          </button>
        </div>
      </div>
    </BaseModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { pharmacyService } from '@/services/agent/pharmacy'
import type { PharmacyMedicine } from '@/types/agent'
import { formatMoney } from '@/utils/format'

interface CartLine extends PharmacyMedicine {
  quantity: number
  specification?: string | null
}

const medicines = ref<PharmacyMedicine[]>([])
const loading = ref(false)
const checkoutLoading = ref(false)
const drawerOpen = ref(false)
const searchText = ref('')
const categoryFilter = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const cartMap = ref<Record<number, CartLine>>({})

const categories = computed(() => Array.from(new Set(medicines.value.map((item) => item.category))).filter(Boolean))

const filteredMedicines = computed(() => medicines.value.filter((item) => {
  const matchSearch = !searchText.value || item.name.toLowerCase().includes(searchText.value.toLowerCase())
  const matchCategory = !categoryFilter.value || item.category === categoryFilter.value
  return matchSearch && matchCategory
}))

const cartItems = computed(() => Object.values(cartMap.value))
const cartCount = computed(() => cartItems.value.reduce((sum, item) => sum + item.quantity, 0))
const cartTotal = computed(() => cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0))

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const loadMedicines = async (): Promise<void> => {
  loading.value = true
  resetMessages()
  try {
    const response = await pharmacyService.listMedicines({ page: 1, page_size: 100 })
    medicines.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取药房数据失败'
  } finally {
    loading.value = false
  }
}

const updateQuantity = (medicineId: number, quantity: number): void => {
  const medicine = medicines.value.find((item) => item.id === medicineId)
  if (!medicine) return

  if (quantity <= 0) {
    const clone = { ...cartMap.value }
    delete clone[medicineId]
    cartMap.value = clone
    return
  }

  cartMap.value = {
    ...cartMap.value,
    [medicineId]: {
      ...medicine,
      quantity: Math.min(quantity, medicine.stock),
      specification: medicine.specification
    }
  }
}

const increaseQuantity = (medicineId: number): void => {
  const current = cartMap.value[medicineId]?.quantity || 0
  updateQuantity(medicineId, current + 1)
}

const decreaseQuantity = (medicineId: number): void => {
  const current = cartMap.value[medicineId]?.quantity || 0
  updateQuantity(medicineId, current - 1)
}

const clearCart = (): void => {
  cartMap.value = {}
}

const checkout = async (): Promise<void> => {
  if (!cartItems.value.length) return
  checkoutLoading.value = true
  resetMessages()
  try {
    const order = await pharmacyService.createOrder(
      cartItems.value.map((item) => ({
        medicine_id: item.id,
        quantity: item.quantity
      }))
    )
    successMessage.value = `订单 ${order.order_no} 已提交，金额 ${formatMoney(order.total_amount)}`
    clearCart()
    drawerOpen.value = false
    await loadMedicines()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '提交订单失败'
  } finally {
    checkoutLoading.value = false
  }
}

const stockLabel = (medicine: PharmacyMedicine): string => {
  if (medicine.stock === 0) return '缺货'
  if (medicine.stock < medicine.threshold) return '库存紧张'
  return '库存充足'
}

const stockTone = (medicine: PharmacyMedicine): 'danger' | 'warning' | 'success' => {
  if (medicine.stock === 0) return 'danger'
  if (medicine.stock < medicine.threshold) return 'warning'
  return 'success'
}

const resetFilters = (): void => {
  searchText.value = ''
  categoryFilter.value = ''
}

onMounted(() => {
  void loadMedicines()
})
</script>

<style scoped>
.quantity-stepper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity-stepper input {
  width: 72px;
  text-align: center;
}
</style>
