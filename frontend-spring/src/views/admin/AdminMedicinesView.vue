<template>
  <div class="page-shell">
    <div class="page-shell__content stack">
      <PageHero
        eyebrow="Admin / Medicines"
        title="药品库存管理"
        description="对接当前 Spring 药品接口，支持新增、编辑和库存增减。当前后端只暴露基础药品字段。"
      >
        <template #actions>
          <button class="button" @click="openMedicineModal()">新增药品</button>
        </template>
      </PageHero>

      <div v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="notice notice--success">{{ successMessage }}</div>

      <section class="glass-card panel stack">
        <div class="toolbar">
          <input v-model.trim="filters.search" placeholder="搜索药品名称" />
          <select v-model="filters.is_active">
            <option value="">全部状态</option>
            <option value="true">启用</option>
            <option value="false">禁用</option>
          </select>
          <button class="button button--secondary" @click="loadMedicines">查询</button>
          <button class="button button--ghost" @click="resetFilters">重置</button>
        </div>

        <table v-if="medicines.length" class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>规格</th>
              <th>单位</th>
              <th>库存</th>
              <th>价格</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="medicine in medicines" :key="medicine.id">
              <td>{{ medicine.name }}</td>
              <td>{{ medicine.spec || '-' }}</td>
              <td>{{ medicine.unit || '-' }}</td>
              <td>{{ medicine.stock }}</td>
              <td>{{ formatMoney(medicine.price) }}</td>
              <td><StatusBadge :label="medicine.is_active ? '启用' : '禁用'" :tone="medicine.is_active ? 'success' : 'default'" /></td>
              <td>
                <div class="action-stack">
                  <button class="link-button" @click="openMedicineModal(medicine)">编辑</button>
                  <button class="link-button" @click="openStockModal(medicine, 1)">入库</button>
                  <button class="link-button" @click="openStockModal(medicine, -1)">盘点/扣减</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="loading" class="empty-state">药品数据加载中...</div>
        <div v-else class="empty-state">暂无药品数据。</div>
      </section>
    </div>

    <BaseModal :open="medicineModalOpen" :title="medicineDraft.id ? '编辑药品' : '新增药品'" eyebrow="Medicine Form" size="sm" @close="medicineModalOpen = false">
      <div class="form-grid">
        <div class="field">
          <label>名称</label>
          <input v-model.trim="medicineDraft.name" />
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>规格</label>
            <input v-model.trim="medicineDraft.spec" />
          </div>
          <div class="field">
            <label>单位</label>
            <input v-model.trim="medicineDraft.unit" />
          </div>
        </div>
        <div class="form-grid form-grid--two">
          <div class="field">
            <label>库存</label>
            <input v-model.number="medicineDraft.stock" type="number" min="0" />
          </div>
          <div class="field">
            <label>价格</label>
            <input v-model.number="medicineDraft.price" type="number" min="0" step="0.01" />
          </div>
        </div>
        <div class="field">
          <label>状态</label>
          <select v-model="medicineDraft.is_active">
            <option :value="true">启用</option>
            <option :value="false">禁用</option>
          </select>
        </div>
        <button class="button" :disabled="actionLoading" @click="submitMedicine">
          {{ actionLoading ? '保存中...' : '保存药品' }}
        </button>
      </div>
    </BaseModal>

    <BaseModal :open="stockModalOpen" title="库存调整" eyebrow="Medicine Stock" size="sm" @close="stockModalOpen = false">
      <div class="form-grid">
        <div class="field">
          <label>变更数量（正数增加，负数减少）</label>
          <input v-model.number="stockDraft.delta" type="number" />
        </div>
        <div class="field">
          <label>原因</label>
          <textarea v-model.trim="stockDraft.reason" rows="3"></textarea>
        </div>
        <button class="button" :disabled="actionLoading || !stockMedicineId" @click="submitStockUpdate">
          {{ actionLoading ? '提交中...' : '提交库存变更' }}
        </button>
      </div>
    </BaseModal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { medicineService, type MedicineResponse } from '@/services/http/medicine'
import { formatMoney } from '@/utils/format'

const medicines = ref<MedicineResponse[]>([])
const loading = ref(false)
const actionLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const medicineModalOpen = ref(false)
const stockModalOpen = ref(false)
const stockMedicineId = ref<number | null>(null)

const filters = reactive({
  search: '',
  is_active: '' as '' | 'true' | 'false'
})

const medicineDraft = reactive({
  id: 0,
  name: '',
  spec: '',
  unit: '',
  stock: 0,
  price: 0,
  is_active: true
})

const stockDraft = reactive({
  delta: 1,
  reason: ''
})

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const loadMedicines = async (): Promise<void> => {
  loading.value = true
  resetMessages()
  try {
    const response = await medicineService.list({
      page: 1,
      page_size: 200,
      search: filters.search || undefined,
      is_active: filters.is_active ? filters.is_active === 'true' : undefined
    })
    medicines.value = response.items
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取药品列表失败'
  } finally {
    loading.value = false
  }
}

const resetFilters = (): void => {
  filters.search = ''
  filters.is_active = ''
  void loadMedicines()
}

const openMedicineModal = (medicine?: MedicineResponse): void => {
  if (medicine) {
    medicineDraft.id = medicine.id
    medicineDraft.name = medicine.name
    medicineDraft.spec = medicine.spec || ''
    medicineDraft.unit = medicine.unit || ''
    medicineDraft.stock = medicine.stock
    medicineDraft.price = Number(medicine.price)
    medicineDraft.is_active = medicine.is_active
  } else {
    medicineDraft.id = 0
    medicineDraft.name = ''
    medicineDraft.spec = ''
    medicineDraft.unit = ''
    medicineDraft.stock = 0
    medicineDraft.price = 0
    medicineDraft.is_active = true
  }
  medicineModalOpen.value = true
}

const submitMedicine = async (): Promise<void> => {
  resetMessages()
  actionLoading.value = true
  try {
    if (medicineDraft.id) {
      await medicineService.update(medicineDraft.id, {
        name: medicineDraft.name,
        spec: medicineDraft.spec || undefined,
        unit: medicineDraft.unit || undefined,
        stock: medicineDraft.stock,
        is_active: medicineDraft.is_active,
        price: medicineDraft.price
      })
      successMessage.value = '药品已更新'
    } else {
      await medicineService.create({
        name: medicineDraft.name,
        spec: medicineDraft.spec || undefined,
        unit: medicineDraft.unit || undefined,
        stock: medicineDraft.stock,
        is_active: medicineDraft.is_active,
        price: medicineDraft.price
      })
      successMessage.value = '药品已创建'
    }
    medicineModalOpen.value = false
    await loadMedicines()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存药品失败'
  } finally {
    actionLoading.value = false
  }
}

const openStockModal = (medicine: MedicineResponse, sign: 1 | -1): void => {
  stockMedicineId.value = medicine.id
  stockDraft.delta = sign
  stockDraft.reason = sign > 0 ? '药品入库' : '库存盘点调整'
  stockModalOpen.value = true
}

const submitStockUpdate = async (): Promise<void> => {
  if (!stockMedicineId.value) return
  resetMessages()
  actionLoading.value = true
  try {
    await medicineService.updateStock(stockMedicineId.value, stockDraft.delta, stockDraft.reason || undefined)
    successMessage.value = '库存已更新'
    stockModalOpen.value = false
    await loadMedicines()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '更新库存失败'
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  void loadMedicines()
})
</script>

<style scoped>
.action-stack {
  display: grid;
  gap: 8px;
}
</style>
