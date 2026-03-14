const baseUrl = process.env.REGRESSION_FRONTEND_BASE_URL || 'http://127.0.0.1:3000'
const webBaseUrl = `${baseUrl}/api/v1`
const agentBaseUrl = `${baseUrl}/agent-api/api/v1`

const ACCOUNTS = {
  admin: { username: 'reg_admin', password: 'test123456', role: 'admin' },
  doctor: { username: 'reg_doctor', password: 'test123456', role: 'doctor' },
  student: { username: 'reg_student', password: 'test123456', role: 'student' }
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitForFrontend() {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    try {
      const response = await fetch(`${baseUrl}/login`)
      if (response.ok) {
        return
      }
    } catch {
      // ignore until retry budget is exhausted
    }
    await sleep(1000)
  }
  throw new Error(`frontend-spring did not become ready at ${baseUrl}`)
}

function buildHeaders({ token, body, extraHeaders } = {}) {
  const headers = {
    Accept: 'application/json',
    ...(extraHeaders || {})
  }

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

async function httpJson(targetBaseUrl, path, { method = 'GET', token, body, extraHeaders } = {}) {
  const response = await fetch(`${targetBaseUrl}${path}`, {
    method,
    headers: buildHeaders({ token, body, extraHeaders }),
    body: body === undefined ? undefined : JSON.stringify(body)
  })

  const text = await response.text()
  let parsed = null

  if (text) {
    try {
      parsed = JSON.parse(text)
    } catch {
      throw new Error(`${method} ${path} returned non-JSON body: ${text}`)
    }
  }

  if (!response.ok) {
    const detail = parsed?.error?.message || parsed?.detail || text
    throw new Error(`${method} ${path} failed with ${response.status}: ${detail}`)
  }

  return parsed
}

async function springRequest(path, options = {}) {
  const payload = await httpJson(webBaseUrl, path, options)
  assert(payload?.success === true, `${options.method || 'GET'} ${path} did not return success=true`)
  return payload.data
}

async function agentRequest(path, options = {}) {
  return httpJson(agentBaseUrl, path, options)
}

async function downloadDocument(path, token) {
  const response = await fetch(`${webBaseUrl}${path}`, {
    method: 'GET',
    headers: buildHeaders({ token })
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(`GET ${path} failed with ${response.status}: ${text}`)
  }

  const contentType = response.headers.get('content-type') || ''
  const buffer = await response.arrayBuffer()
  assert(buffer.byteLength > 0, `${path} returned empty body`)
  return {
    contentType,
    size: buffer.byteLength
  }
}

async function login(account) {
  const data = await springRequest('/auth/login', {
    method: 'POST',
    body: {
      username: account.username,
      password: account.password,
      role: account.role
    }
  })

  assert(data.access_token, `login for ${account.username} did not return access_token`)
  return data
}

function formatDate(date) {
  return date.toISOString().slice(0, 10)
}

function parseSlotMinutes(timeSlot) {
  const start = String(timeSlot).split('-')[0].split('~')[0].trim()
  const [hours, minutes] = start.split(':').map((value) => Number.parseInt(value, 10))
  return (hours * 60) + minutes
}

function isPastOrCurrentSlot(slot, now = new Date()) {
  const today = formatDate(now)
  if (slot.date !== today) {
    return false
  }

  const currentMinutes = (now.getHours() * 60) + now.getMinutes()
  return parseSlotMinutes(slot.time) <= currentMinutes
}

async function findRegressionDepartment() {
  const departments = await springRequest('/public/departments')
  assert(Array.isArray(departments.items), 'public departments response invalid')
  const department = departments.items.find((item) => item.name === 'Regression Dept')
  assert(department, 'Regression Dept was not found in public departments')
  return department
}

async function getPublicDoctorsByDepartment(departmentId) {
  const response = await springRequest(`/public/doctors?department_id=${departmentId}&page=1&page_size=100`)
  assert(Array.isArray(response.items), 'public doctors response invalid')
  return response.items
}

async function getAdminDoctor(adminToken) {
  const response = await springRequest('/doctors?page=1&page_size=100&search=REGD001', {
    token: adminToken
  })
  assert(Array.isArray(response.items), 'admin doctors response invalid')
  const doctor = response.items.find((item) => item.username === ACCOUNTS.doctor.username)
  assert(doctor, 'reg_doctor not found in admin doctor list')
  return doctor
}

async function getAdminStudent(adminToken) {
  const response = await springRequest('/students?page=1&page_size=100&search=REGS2026001', {
    token: adminToken
  })
  assert(Array.isArray(response.items), 'admin students response invalid')
  const student = response.items.find((item) => item.username === ACCOUNTS.student.username)
  assert(student, 'reg_student not found in admin student list')
  return student
}

async function setDoctorStatus(adminToken, doctorId, isActive) {
  return springRequest(`/doctors/${doctorId}/status`, {
    method: 'PATCH',
    token: adminToken,
    body: { is_active: isActive }
  })
}

async function bulkOpenSchedules(adminToken, doctorId, dateFrom, dateTo) {
  return springRequest('/doctor-schedules/bulk', {
    method: 'POST',
    token: adminToken,
    body: {
      doctor_id: doctorId,
      date_from: dateFrom,
      date_to: dateTo,
      time_slots: ['00:00', '08:00', '10:00', '14:00', '16:00'],
      capacity: 8,
      status: 'open'
    }
  })
}

async function listSchedules(adminToken, doctorId, dateFrom, dateTo) {
  const query = new URLSearchParams({
    doctor_id: String(doctorId),
    date_from: dateFrom,
    date_to: dateTo,
    page: '1',
    page_size: '200'
  })
  return springRequest(`/doctor-schedules?${query.toString()}`, { token: adminToken })
}

async function getAvailableSlots(studentToken, doctorId, dateFrom, dateTo) {
  const query = new URLSearchParams({
    doctor_id: String(doctorId),
    date_from: dateFrom,
    date_to: dateTo
  })
  const slots = await springRequest(`/appointments/slots?${query.toString()}`, { token: studentToken })
  assert(Array.isArray(slots), 'appointment slots response invalid')
  return slots
}

async function createAppointment(studentToken, payload) {
  return springRequest('/appointments', {
    method: 'POST',
    token: studentToken,
    body: payload
  })
}

async function listMyAppointments(studentToken, params = {}) {
  const query = new URLSearchParams({
    page: String(params.page || 1),
    page_size: String(params.page_size || 50)
  })
  if (params.status) query.set('status', params.status)
  return springRequest(`/appointments/mine?${query.toString()}`, { token: studentToken })
}

async function cancelAppointment(studentToken, appointmentId, reason) {
  return springRequest(`/appointments/${appointmentId}/cancel`, {
    method: 'POST',
    token: studentToken,
    body: { reason }
  })
}

async function rescheduleAppointment(studentToken, appointmentId, newDate, newTime, reason) {
  return springRequest(`/appointments/${appointmentId}/reschedule`, {
    method: 'POST',
    token: studentToken,
    body: {
      new_date: newDate,
      new_time: newTime,
      reason
    }
  })
}

async function listDoctorAppointments(doctorToken, status) {
  const query = new URLSearchParams()
  if (status) query.set('status', status)
  const suffix = query.toString() ? `?${query.toString()}` : ''
  return springRequest(`/doctor/appointments${suffix}`, { token: doctorToken })
}

async function startConsultation(doctorToken, appointmentId) {
  return springRequest(`/doctor/appointments/${appointmentId}/start`, {
    method: 'POST',
    token: doctorToken
  })
}

async function getAppointmentHistory(doctorToken, appointmentId) {
  return springRequest(`/doctor/appointments/${appointmentId}/history?page=1&page_size=10`, {
    token: doctorToken
  })
}

async function submitDiagnosis(doctorToken, appointmentId, medicineId) {
  return springRequest(`/doctor/appointments/${appointmentId}/diagnosis`, {
    method: 'POST',
    token: doctorToken,
    body: {
      category: 'General Medicine',
      signs: 'Stable vitals',
      conclusion: 'Frontend regression diagnosis completed',
      advice: 'Rest and hydration',
      drug_ids: medicineId ? [medicineId] : [],
      items: []
    }
  })
}

async function listMedicalRecords(studentToken) {
  return springRequest('/medical-records/mine?page=1&page_size=20', { token: studentToken })
}

async function listDocuments(studentToken, appointmentId) {
  return springRequest(`/appointments/${appointmentId}/documents`, { token: studentToken })
}

async function listDoctorMedicines(doctorToken) {
  return springRequest('/medicines?page=1&page_size=50', { token: doctorToken })
}

async function listAgentMedicines(studentToken) {
  return agentRequest('/pharmacy/medicines?page=1&page_size=20', { token: studentToken })
}

async function createAgentOrder(studentToken, medicineId) {
  return agentRequest('/pharmacy/orders', {
    method: 'POST',
    token: studentToken,
    body: {
      items: [{ medicine_id: medicineId, quantity: 1 }]
    }
  })
}

async function listAgentOrders(studentToken) {
  return agentRequest('/pharmacy/orders?page=1&page_size=20', { token: studentToken })
}

async function getAgentOrder(studentToken, orderId) {
  return agentRequest(`/pharmacy/orders/${orderId}`, { token: studentToken })
}

async function main() {
  await waitForFrontend()

  const summary = {
    frontend_base_url: baseUrl,
    proxy_base_urls: {
      web: webBaseUrl,
      agent: agentBaseUrl
    },
    accounts: {
      admin: ACCOUNTS.admin.username,
      doctor: ACCOUNTS.doctor.username,
      student: ACCOUNTS.student.username
    },
    role_checks: {},
    cross_role: {},
    artifacts: {}
  }

  const adminLogin = await login(ACCOUNTS.admin)
  const doctorLogin = await login(ACCOUNTS.doctor)
  const studentLogin = await login(ACCOUNTS.student)

  const adminToken = adminLogin.access_token
  const doctorToken = doctorLogin.access_token
  const studentToken = studentLogin.access_token

  const [adminMe, doctorMe, studentMe] = await Promise.all([
    springRequest('/auth/me', { token: adminToken }),
    springRequest('/auth/me', { token: doctorToken }),
    springRequest('/auth/me', { token: studentToken })
  ])

  assert(adminMe.role === 'admin', 'reg_admin role mismatch')
  assert(doctorMe.role === 'doctor', 'reg_doctor role mismatch')
  assert(studentMe.role === 'student', 'reg_student role mismatch')

  const regressionDepartment = await findRegressionDepartment()
  const adminDoctor = await getAdminDoctor(adminToken)
  const adminStudent = await getAdminStudent(adminToken)

  summary.role_checks = {
    me_verified: true,
    department_id: regressionDepartment.id,
    doctor_id: adminDoctor.id,
    student_id: adminStudent.id
  }

  const publicDoctorsBefore = await getPublicDoctorsByDepartment(regressionDepartment.id)
  assert(publicDoctorsBefore.some((doctor) => doctor.id === adminDoctor.id), 'doctor is not visible before status toggle')

  await setDoctorStatus(adminToken, adminDoctor.id, false)
  const publicDoctorsDisabled = await getPublicDoctorsByDepartment(regressionDepartment.id)
  assert(!publicDoctorsDisabled.some((doctor) => doctor.id === adminDoctor.id), 'disabled doctor is still visible')

  await setDoctorStatus(adminToken, adminDoctor.id, true)
  const publicDoctorsEnabled = await getPublicDoctorsByDepartment(regressionDepartment.id)
  assert(publicDoctorsEnabled.some((doctor) => doctor.id === adminDoctor.id), 're-enabled doctor is not visible')

  const dateFrom = formatDate(new Date())
  const dateTo = formatDate(new Date(Date.now() + (6 * 24 * 60 * 60 * 1000)))

  await bulkOpenSchedules(adminToken, adminDoctor.id, dateFrom, dateTo)
  const schedules = await listSchedules(adminToken, adminDoctor.id, dateFrom, dateTo)
  assert(Array.isArray(schedules.items) && schedules.items.length > 0, 'schedule list is empty after bulk upsert')

  const availableSlots = await getAvailableSlots(studentToken, adminDoctor.id, dateFrom, dateTo)
  const candidateSlot = availableSlots.find((slot) => slot.status === 'available' && !isPastOrCurrentSlot(slot))
  assert(candidateSlot, 'no available future appointment slot found')

  const createdAppointment = await createAppointment(studentToken, {
    doctor_id: adminDoctor.id,
    date: candidateSlot.date,
    time: candidateSlot.time,
    symptoms: 'frontend proxy regression appointment'
  })
  assert(createdAppointment.id, 'appointment creation did not return id')

  const studentAppointments = await listMyAppointments(studentToken)
  assert(studentAppointments.items.some((item) => item.id === createdAppointment.id), 'created appointment missing from student list')

  const doctorWaiting = await listDoctorAppointments(doctorToken, 'confirmed')
  assert(doctorWaiting.items.some((item) => item.appointment_id === createdAppointment.id), 'doctor waiting list missing created appointment')

  await startConsultation(doctorToken, createdAppointment.id)
  const doctorHistory = await getAppointmentHistory(doctorToken, createdAppointment.id)
  assert(doctorHistory.student.id === adminStudent.id, 'doctor history did not resolve correct student')

  const doctorMedicines = await listDoctorMedicines(doctorToken)
  const medicineId = doctorMedicines.items[0]?.id
  const diagnosis = await submitDiagnosis(doctorToken, createdAppointment.id, medicineId)
  assert(diagnosis.appointment_id === createdAppointment.id, 'diagnosis response appointment mismatch')

  const medicalRecords = await listMedicalRecords(studentToken)
  const newMedicalRecord = medicalRecords.items.find((record) => record.appointment_id === createdAppointment.id)
  assert(newMedicalRecord, 'new medical record not found after diagnosis')

  const documents = await listDocuments(studentToken, createdAppointment.id)
  assert(Array.isArray(documents.items) && documents.items.length >= 1, 'appointment documents not generated')

  const downloaded = []
  for (const document of documents.items) {
    const query = new URLSearchParams({ doc_type: document.doc_type })
    const result = await downloadDocument(`/appointments/${createdAppointment.id}/documents/download?${query.toString()}`, studentToken)
    downloaded.push({
      doc_type: document.doc_type,
      content_type: result.contentType,
      size: result.size
    })
  }

  const rescheduleSlots = await getAvailableSlots(studentToken, adminDoctor.id, dateFrom, dateTo)
  const rescheduleTarget = rescheduleSlots.find((slot) =>
    slot.status === 'available' &&
    `${slot.date}-${slot.time}` !== `${candidateSlot.date}-${candidateSlot.time}` &&
    !isPastOrCurrentSlot(slot)
  )
  assert(rescheduleTarget, 'no alternative slot found for reschedule')

  const rescheduledAppointment = await createAppointment(studentToken, {
    doctor_id: adminDoctor.id,
    date: rescheduleTarget.date,
    time: rescheduleTarget.time,
    symptoms: 'frontend proxy regression reschedule candidate'
  })
  await rescheduleAppointment(
    studentToken,
    rescheduledAppointment.id,
    candidateSlot.date,
    candidateSlot.time,
    'frontend proxy regression reschedule'
  )

  const cancellableAppointment = await createAppointment(studentToken, {
    doctor_id: adminDoctor.id,
    date: rescheduleTarget.date,
    time: rescheduleTarget.time,
    symptoms: 'frontend proxy regression cancel candidate'
  })
  await cancelAppointment(studentToken, cancellableAppointment.id, 'frontend proxy regression cleanup')

  const agentMedicines = await listAgentMedicines(studentToken)
  assert(Array.isArray(agentMedicines.items) && agentMedicines.items.length > 0, 'agent medicines response invalid')
  const agentMedicineId = agentMedicines.items[0].id

  const createdOrder = await createAgentOrder(studentToken, agentMedicineId)
  assert(createdOrder.id, 'agent order creation did not return id')

  const agentOrders = await listAgentOrders(studentToken)
  assert(agentOrders.items.some((order) => order.id === createdOrder.id), 'agent order missing from order list')
  const agentOrderDetail = await getAgentOrder(studentToken, createdOrder.id)
  assert(agentOrderDetail.id === createdOrder.id, 'agent order detail mismatch')

  summary.cross_role = {
    doctor_visibility_toggle_tested: true,
    schedule_bulk_upsert_tested: true,
    appointment_created: createdAppointment.id,
    diagnosis_completed: true,
    reschedule_tested: true,
    cancel_tested: true,
    agent_order_tested: true
  }

  summary.artifacts = {
    appointment_id: createdAppointment.id,
    medical_record_id: newMedicalRecord.id,
    downloaded_documents: downloaded,
    rescheduled_appointment_id: rescheduledAppointment.id,
    cancelled_appointment_id: cancellableAppointment.id,
    agent_order_id: createdOrder.id
  }

  console.log(JSON.stringify(summary, null, 2))
}

main().catch((error) => {
  console.error(error.stack || error.message || String(error))
  process.exit(1)
})
