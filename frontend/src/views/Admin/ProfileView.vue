<template>
  <div class="profile-view">
    <el-card shadow="never" class="profile-card">
      <template #header>
        <div class="card-header">
          <span class="title">基本资料</span>
        </div>
      </template>

      <!-- 只读区：用户名/学号/班级由教务或管理员确定，本人不可改 -->
      <el-descriptions :column="4" border class="readonly-block">
        <el-descriptions-item label="用户名">{{ user?.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ user?.studentNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ user?.orgName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ formatTime(user?.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <!-- 可编辑区 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        class="profile-form"
        @submit.prevent="handleSave"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择" class="full-width">
            <el-option
              v-for="item in GENDER_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="出生日期" prop="birthday">
          <el-date-picker
            v-model="form.birthday"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期"
            class="full-width"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="20" placeholder="选填" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" maxlength="100" placeholder="选填" />
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button type="primary" :disabled="!dirty" :loading="saving" @click="handleSave">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <!--
        改密默认收起 —— 它是个低频操作，常驻展开会把「看自己资料」这件事挤到次要位置。
      -->
      <el-button @click="togglePassword">{{ pwdVisible ? '收起' : '修改密码' }}</el-button>

      <div v-if="pwdVisible" class="password-block">
        <p class="hint">修改成功后需要重新登录。新密码需 6-20 位，且同时包含字母和数字。</p>

        <el-form
          ref="pwdFormRef"
          :model="pwdForm"
          :rules="pwdRules"
          label-width="90px"
          @submit.prevent="handleChangePassword"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="pwdForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="pwdForm.newPassword"
              type="password"
              show-password
              placeholder="6-20位，含字母和数字"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="pwdForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
              @keyup.enter="handleChangePassword"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleChangePassword">
              确认修改
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword, updateProfile } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { PASSWORD_PATTERN, PASSWORD_MESSAGE } from '@/constants/password-rule'
import {
  PHONE_PATTERN,
  PHONE_MESSAGE,
  EMAIL_PATTERN,
  EMAIL_MESSAGE,
  GENDER_OPTIONS
} from '@/constants/validation-rule'

const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const formRef = ref(null)
const saving = ref(false)

const form = reactive({
  name: '',
  gender: '',
  birthday: '',
  phone: '',
  email: ''
})

/** 用 store 里的用户信息重建表单（进页面时、保存成功后各调一次） */
function initForm() {
  const u = authStore.user || {}
  form.name = u.name || ''
  // gender 在库里是 TINYINT，后端经实体映射回来是字符串，但保险起见统一成字符串比较
  form.gender = u.gender == null ? '' : String(u.gender)
  form.birthday = u.birthday || ''
  form.phone = u.phone || ''
  form.email = u.email || ''
}

onMounted(initForm)

/** 有改动才允许保存，避免用户点了个没变化的「保存修改」却收到成功提示 */
const dirty = computed(() => {
  const u = authStore.user
  if (!u) return false
  return (
    form.name !== (u.name || '') ||
    form.gender !== (u.gender == null ? '' : String(u.gender)) ||
    form.birthday !== (u.birthday || '') ||
    form.phone !== (u.phone || '') ||
    form.email !== (u.email || '')
  )
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: PHONE_PATTERN, message: PHONE_MESSAGE, trigger: 'blur' }],
  email: [{ pattern: EMAIL_PATTERN, message: EMAIL_MESSAGE, trigger: 'blur' }]
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const updated = await updateProfile({
      name: form.name.trim(),
      gender: form.gender,
      // 清空日期选择器会得到 null，后端 LocalDate 接受 null
      birthday: form.birthday || null,
      phone: form.phone.trim(),
      email: form.email.trim()
    })

    // 以后端返回值为准重建表单 —— 后端会把空白归一成 null，本地再算一遍容易不一致
    authStore.setUser(updated)
    initForm()
    formRef.value?.clearValidate()
    ElMessage.success('资料已保存')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ───────────────────────── 修改密码 ─────────────────────────

const pwdVisible = ref(false)
const pwdFormRef = ref(null)
const submitting = ref(false)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: PASSWORD_PATTERN, message: PASSWORD_MESSAGE, trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

function togglePassword() {
  if (pwdVisible.value) {
    // 收起时清空 —— 否则下次展开会看到上次输入到一半的密码
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdFormRef.value?.clearValidate()
  }
  pwdVisible.value = !pwdVisible.value
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm('修改成功后需要重新登录，确定继续？', '请确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }

  submitting.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })

    ElMessage.success('密码已修改，请用新密码登录')
    // 强制重新登录：既符合常规预期，也让用户立即验证新密码可用
    authStore.clearAuth()
    router.push('/')
  } catch (err) {
    // 原密码错误、新旧相同等都由服务端判定，直接展示后端文案
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 19)
}
</script>

<style scoped>
/* 卡片铺满整宽，与其他后台页面（用户管理 / 成绩 / 组织）保持一致，
   不做居中 —— 居中会在两侧同时留白，和这些页面对不齐 */
.profile-card {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
}

.card-header .title {
  font-size: 15px;
  font-weight: 600;
  color: var(--admin-text);
}

.readonly-block :deep(.el-descriptions__label) {
  width: 90px;
  color: var(--admin-text-muted);
}

/* 可编辑字段排两列：单列会让输入框在宽屏下被拉成近 900px，读起来很别扭 */
.profile-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 24px;
}

/* 保存按钮横跨两列，不占单独的半格 */
.profile-form .form-actions {
  grid-column: 1 / -1;
}

.full-width {
  width: 100%;
}

/* el-date-picker 的根元素自带 `.el-date-editor.el-input` 两条类，权重高于上面的
   `.full-width[data-v-x]`，直接用 :deep 提一级才能盖住它的固定 220px 宽度 */
.profile-form :deep(.el-date-editor) {
  width: 100%;
}

/* 改密是收起状态的次要操作，限宽避免输入框拉得过长 */
.password-block {
  max-width: 560px;
  margin-top: 20px;
}

.hint {
  margin: 0 0 20px;
  color: var(--admin-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

/* 窄屏放不下两列时退回单列 */
@media (max-width: 900px) {
  .profile-form {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
