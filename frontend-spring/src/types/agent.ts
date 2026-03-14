export interface SlotCandidate {
  date: string
  time: string
  capacity: number
  booked_count: number
}

export interface DoctorCard {
  doctor_id: number
  doctor_name: string
  department: string
  title: string
  introduction: string
  recommend_reason: string
  slot_candidates: SlotCandidate[]
}

export interface MedicineCard {
  medicine_id: number
  name: string
  spec?: string | null
  unit?: string | null
  price: number
  stock: number
  default_quantity: number
  max_quantity: number
  recommend_reason: string
}

export interface AssistantStatePayload {
  public_session_id: string
  intent: 'diagnosis_only' | 'appointment_only' | 'medicine_only' | 'appointment_and_medicine'
  answer: string
  reasoning_summary: string
  doctor_cards: DoctorCard[]
  medicine_cards: MedicineCard[]
}

export interface AssistantActionResponse {
  success: boolean
  message: string
  data?: Record<string, unknown> | null
}

export interface ChatSessionItem {
  public_session_id: string
  title?: string | null
  status: string
  last_message_at: string
  created_at: string
}

export interface ChatMessageItem {
  seq_no: number
  role: string
  message_kind: string
  text: string
  cards?: Record<string, unknown> | null
  action_payload?: Record<string, unknown> | null
  created_at: string
}

export interface PharmacyMedicine {
  id: number
  code?: string | null
  name: string
  category: string
  specification?: string | null
  unit?: string | null
  stock: number
  threshold: number
  price: number
  supplier?: string | null
  manufacturer?: string | null
  approval_number?: string | null
  is_active: boolean
}
