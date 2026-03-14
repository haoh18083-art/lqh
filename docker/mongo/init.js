db = db.getSiblingDB('campus_medical');

const appUser = 'medical_app';
const appPassword = 'medical_app_password';

if (!db.getUser(appUser)) {
  db.createUser({
    user: appUser,
    pwd: appPassword,
    roles: [{ role: 'readWrite', db: 'campus_medical' }]
  });
}

[
  'medical_records',
  'lab_results',
  'medical_images',
  'system_logs',
  'audit_logs',
  'notifications',
  'health_profiles',
  'health_metrics',
  'cache',
  'sessions',
  'system_config',
  'ai_conversations',
  'ai_messages'
].forEach((name) => {
  if (db.getCollectionNames().includes(name)) {
    db.getCollection(name).drop();
  }
});

db.createCollection('medical_records');
db.medical_records.createIndex({ student_id: 1, visit_date: -1 });
db.medical_records.createIndex({ appointment_id: 1 }, { unique: true });
db.medical_records.createIndex({ doctor_id: 1 });
db.medical_records.createIndex({ created_at: -1 });

db.createCollection('lab_results');
db.lab_results.createIndex({ patient_id: 1 });
db.lab_results.createIndex({ appointment_id: 1 });

db.createCollection('medical_images');
db.medical_images.createIndex({ patient_id: 1 });
db.medical_images.createIndex({ appointment_id: 1 });

db.createCollection('system_logs');
db.system_logs.createIndex({ level: 1 });
db.system_logs.createIndex({ created_at: -1 });

db.createCollection('audit_logs');
db.audit_logs.createIndex({ user_id: 1 });
db.audit_logs.createIndex({ action: 1 });
db.audit_logs.createIndex({ created_at: -1 });

db.createCollection('notifications');
db.notifications.createIndex({ user_id: 1 });
db.notifications.createIndex({ is_read: 1 });
db.notifications.createIndex({ created_at: -1 });

db.createCollection('health_profiles');
db.health_profiles.createIndex({ user_id: 1 }, { unique: true });

db.createCollection('health_metrics');
db.health_metrics.createIndex({ user_id: 1, recorded_at: -1 });

db.createCollection('cache');
db.cache.createIndex({ key: 1 }, { unique: true });
db.cache.createIndex({ expiry: 1 }, { expireAfterSeconds: 0 });

db.createCollection('sessions');
db.sessions.createIndex({ session_id: 1 }, { unique: true });
db.sessions.createIndex({ expires_at: 1 }, { expireAfterSeconds: 0 });

db.createCollection('system_config');
db.system_config.createIndex({ key: 1 }, { unique: true });

db.createCollection('ai_conversations');
db.ai_conversations.createIndex({ public_session_id: 1 }, { unique: true });
db.ai_conversations.createIndex({ student_id: 1, updated_at: -1 });

db.createCollection('ai_messages');
db.ai_messages.createIndex({ conversation_id: 1, seq_no: 1 }, { unique: true });
db.ai_messages.createIndex({ session_mysql_id: 1, seq_no: 1 });

function daysAgo(days, hour, minute) {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() - days);
  date.setUTCHours(hour || 0, minute || 0, 0, 0);
  return date;
}

function daysAhead(days, hour, minute) {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + days);
  date.setUTCHours(hour || 0, minute || 0, 0, 0);
  return date;
}

function dateOnly(date) {
  return date.toISOString().slice(0, 10);
}

const visitA = daysAgo(60, 1, 35);
const visitB = daysAgo(28, 6, 30);
const visitC = daysAgo(8, 2, 20);
const visitD = daysAgo(5, 1, 0);

db.health_profiles.insertMany([
  {
    user_id: 2,
    blood_type: 'A',
    last_checkup_date: daysAgo(90, 0, 0),
    allergies: ['青霉素'],
    medical_history: [
      {
        condition: '过敏性鼻炎',
        date: daysAgo(120, 0, 0),
        notes: '换季时加重'
      }
    ],
    emergency_contact: {
      name: '张家长',
      phone: '13900009999',
      relationship: '母亲'
    },
    created_at: daysAgo(120, 0, 0),
    updated_at: daysAgo(5, 0, 0)
  },
  {
    user_id: 5,
    blood_type: 'O',
    last_checkup_date: daysAgo(30, 0, 0),
    allergies: [],
    medical_history: [],
    emergency_contact: {
      name: 'Li Parent',
      phone: '13800008888',
      relationship: 'Father'
    },
    created_at: daysAgo(40, 0, 0),
    updated_at: daysAgo(3, 0, 0)
  }
]);

db.medical_records.insertMany([
  {
    appointment_id: 1,
    student_id: 1,
    doctor_id: 1,
    department_id: 1,
    visit_date: dateOnly(visitA),
    time_slot: '09:00',
    symptoms: '发热、咳嗽两天',
    doctor_name: '测试医生',
    department_name: '内科',
    diagnosis_summary: {
      category: '上呼吸道感染',
      signs: '38.2℃，咽部充血',
      conclusion: '普通感冒伴低热',
      advice: '多喝温水，注意休息，若持续高热请复诊。'
    },
    prescription_summary: [
      {
        name: '对乙酰氨基酚片',
        dosage: '每次1片，每日3次',
        quantity: 2,
        unit: '盒',
        unit_price: 28.5,
        total_price: 57.0
      },
      {
        name: '布洛芬缓释胶囊',
        dosage: '发热时每次1粒',
        quantity: 1,
        unit: '盒',
        unit_price: 16.0,
        total_price: 16.0
      }
    ],
    fee_total: 73.0,
    created_at: visitA
  },
  {
    appointment_id: 2,
    student_id: 1,
    doctor_id: 1,
    department_id: 1,
    visit_date: dateOnly(visitB),
    time_slot: '14:00',
    symptoms: '腹痛、腹泻',
    doctor_name: '测试医生',
    department_name: '内科',
    diagnosis_summary: {
      category: '急性胃肠炎',
      signs: '腹部轻压痛，无脱水体征',
      conclusion: '胃肠功能紊乱',
      advice: '清淡饮食，避免辛辣生冷，若腹泻加重请复诊。'
    },
    prescription_summary: [],
    fee_total: 0,
    created_at: visitB
  },
  {
    appointment_id: 3,
    student_id: 1,
    doctor_id: 1,
    department_id: 1,
    visit_date: dateOnly(visitC),
    time_slot: '10:00',
    symptoms: '鼻塞、打喷嚏、眼痒',
    doctor_name: '测试医生',
    department_name: '内科',
    diagnosis_summary: {
      category: '过敏性鼻炎',
      signs: '鼻甲肿胀，结膜轻度充血',
      conclusion: '季节性过敏性鼻炎',
      advice: '减少接触过敏原，症状明显时规律服药。'
    },
    prescription_summary: [
      {
        name: '盐酸西替利嗪片',
        dosage: '每晚1片',
        quantity: 1,
        unit: '盒',
        unit_price: 18.5,
        total_price: 18.5
      }
    ],
    fee_total: 18.5,
    created_at: visitC
  },
  {
    appointment_id: 6,
    student_id: 2,
    doctor_id: 3,
    department_id: 6,
    visit_date: dateOnly(visitD),
    time_slot: '08:30',
    symptoms: '孩子咳嗽、轻度腹泻',
    doctor_name: '周磊',
    department_name: '儿科',
    diagnosis_summary: {
      category: '小儿普通感冒',
      signs: '轻度咽红，偶有腹泻',
      conclusion: '儿童上呼吸道感染',
      advice: '补充水分，观察精神状态和食欲变化。'
    },
    prescription_summary: [
      {
        name: '对乙酰氨基酚片',
        dosage: '体温超过38.5℃时每次半片',
        quantity: 1,
        unit: '盒',
        unit_price: 28.5,
        total_price: 28.5
      },
      {
        name: '蒙脱石散',
        dosage: '每日3次，每次1袋',
        quantity: 1,
        unit: '盒',
        unit_price: 12.5,
        total_price: 12.5
      }
    ],
    fee_total: 41.0,
    created_at: visitD
  }
]);

db.system_config.insertOne({
  key: 'appointment_settings',
  value: {
    advance_days: 7,
    time_slots: ['08:00', '08:30', '09:00', '10:00', '13:30', '14:00', '16:00'],
    max_daily_appointments: 50
  },
  updated_at: new Date()
});

db.audit_logs.insertMany([
  {
    user_id: 1,
    action: 'seed_init',
    resource_type: 'system',
    resource_id: 'docker-bootstrap',
    detail: 'Initialized deterministic Docker test dataset',
    created_at: new Date()
  },
  {
    user_id: 3,
    action: 'submit_diagnosis',
    resource_type: 'appointment',
    resource_id: 3,
    detail: 'Seeded historical diagnosis for appointment 3',
    created_at: visitC
  }
]);

db.notifications.insertOne({
  user_id: 2,
  type: 'appointment',
  title: '预约提醒',
  content: '你在明天 08:00 有一条待就诊预约。',
  is_read: false,
  created_at: daysAhead(0, 0, 0)
});

db.ai_conversations.insertOne({
  _id: ObjectId('65f0a0000000000000000001'),
  public_session_id: '11111111-2222-3333-4444-555555555555',
  student_id: 1,
  created_at: daysAgo(1, 2, 0),
  updated_at: daysAgo(1, 2, 5)
});

db.ai_messages.insertMany([
  {
    _id: ObjectId('65f0b0000000000000000001'),
    conversation_id: ObjectId('65f0a0000000000000000001'),
    session_mysql_id: 1,
    message_meta_mysql_id: 1,
    seq_no: 1,
    role: 'user',
    text: '头痛应该挂什么科？',
    cards: null,
    action_payload: null,
    created_at: daysAgo(1, 2, 0)
  },
  {
    _id: ObjectId('65f0b0000000000000000002'),
    conversation_id: ObjectId('65f0a0000000000000000001'),
    session_mysql_id: 1,
    message_meta_mysql_id: 2,
    seq_no: 2,
    role: 'assistant',
    text: '如果伴随发热、咽痛或全身不适，建议优先挂内科。',
    cards: {
      intent: 'appointment_recommendation',
      reasoning_summary: '症状属于校园门诊内科常见就诊范围。',
      doctor_cards: [
        {
          doctor_id: 1,
          doctor_name: '测试医生',
          department_name: '内科'
        }
      ],
      medicine_cards: []
    },
    action_payload: null,
    created_at: daysAgo(1, 2, 5)
  }
]);

print('MongoDB test dataset initialized.');
