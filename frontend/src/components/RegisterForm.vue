<template>
  <div class="register-card">
    <div class="brand">
      <img :src="Images.mjuLogo" class="brand-logo" alt="校徽" />
      <h2 class="brand-title">学生注册</h2>
      <p class="brand-sub">请填写完整信息完成注册</p>
    </div>

    <el-form
      :model="form"
      :rules="rules"
      ref="formRef"
      label-position="top"
      @submit.prevent="handleRegister"
    >
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名" size="large" />
      </el-form-item>
      <el-form-item prop="name">
        <el-input v-model="form.name" placeholder="姓名" size="large" />
      </el-form-item>
      <el-form-item prop="studentNo">
        <el-input v-model="form.studentNo" placeholder="学号（登录账号）" size="large" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="密码（6-20位，含字母和数字）"
          size="large"
        />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="确认密码"
          size="large"
        />
      </el-form-item>
      <el-form-item prop="gender">
        <el-select
          v-model="form.gender"
          placeholder="请选择性别"
          size="large"
          class="full-width"
          popper-class="register-gender-popper"
        >
          <el-option label="男" value="1" />
          <el-option label="女" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item prop="orgId">
        <el-popover
          v-model:visible="orgVisible"
          placement="bottom-start"
          trigger="click"
          :width="320"
        >
          <template #reference>
            <el-input
              v-model="orgLabel"
              placeholder="请选择所属班级"
              readonly
              size="large"
              class="org-input"
            >
              <template #suffix>
                <span class="org-caret">▾</span>
              </template>
            </el-input>
          </template>
          <div class="org-tree">
            <el-tree
              :data="orgTree"
              :props="treeProps"
              node-key="id"
              default-expand-all
              highlight-current
              :expand-on-click-node="false"
              @node-click="handleOrgClick"
            >
              <template #default="{ data }">
                <span :class="{ 'org-class': data.type === 'class' }">{{ data.name }}</span>
              </template>
            </el-tree>
            <div class="org-hint">请选择到最低一级（班级）</div>
          </div>
        </el-popover>
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="手机号（选填）" size="large" />
      </el-form-item>
      <el-form-item prop="email">
        <el-input v-model="form.email" placeholder="邮箱（选填）" size="large" />
      </el-form-item>
      <el-form-item prop="birthday">
        <el-date-picker
          v-model="form.birthday"
          type="date"
          placeholder="出生年月（选填）"
          value-format="YYYY-MM-DD"
          size="large"
          class="full-width"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="success"
          size="large"
          class="register-btn"
          @click="handleRegister"
          :loading="loading"
        >
          注 册
        </el-button>
      </el-form-item>
    </el-form>

    <div class="login-link" @click="goLogin">已有账号？立即登录</div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { PASSWORD_PATTERN, PASSWORD_MESSAGE } from '@/constants/password-rule'
import Images from '@/constants/images'
import { getOrgTree } from '@/api/organization'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
  studentNo: '',
  gender: '',
  orgId: '',
  phone: '',
  email: '',
  birthday: null
})

// 组织架构树
const orgTree = ref([])
const orgVisible = ref(false)
const orgLabel = ref('')
const treeProps = { label: 'name', children: 'children' }

const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    // 强度规则与后端 PasswordPolicy 一致，见 @/constants/password-rule
    {
      pattern: PASSWORD_PATTERN,
      message: PASSWORD_MESSAGE,
      trigger: 'blur'
    }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  orgId: [{ required: true, message: '请选择所属班级', trigger: 'change' }]
}

function handleOrgClick(data) {
  // 组织架构树只允许选到最低层（班级）
  if (data.type !== 'class') return
  form.orgId = data.id
  orgLabel.value = data.name
  orgVisible.value = false
  formRef.value?.validateField('orgId').catch(() => {})
}

async function loadOrgTree() {
  try {
    orgTree.value = await getOrgTree()
  } catch (err) {
    // 组织树加载失败不阻塞注册流程，但无法选班级，给出提示
    alert('组织架构加载失败：' + (err.response?.data?.message || err.message))
  }
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.register(form)
    alert('注册成功，请登录')
    router.push('/')
  } catch (err) {
    alert('注册失败：' + (err.response?.data?.message || err.message))
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/')
}

onMounted(loadOrgTree)
</script>

<style scoped>
/* 配色取自管理员后台主题：青绿 #1ab394 + 浅色卡片 */
.register-card {
  width: 420px;
  padding: 40px 40px 30px;
  background: #fff;
  border: 1px solid var(--admin-border, #ebeef5);
  border-radius: 12px;
  box-shadow: 0 14px 40px rgba(26, 179, 148, 0.12);

  /* 管理员后台主色（青绿），只在注册卡片内生效 */
  --el-color-primary: #1ab394;
  --el-color-primary-light-3: #4fc4ab;
  --el-color-primary-light-5: #8dd9cb;
  --el-color-primary-light-7: #bce9e0;
  --el-color-primary-light-8: #d3f1eb;
  --el-color-primary-light-9: #e9f8f5;
  --el-color-primary-dark-2: #159c81;
}

.brand {
  text-align: center;
  margin-bottom: 24px;
}

.brand-logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
  margin-bottom: 8px;
}

.brand-title {
  margin: 0;
  font-size: 19px;
  line-height: 1.5;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--admin-text, #303133);
}

.brand-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--admin-text-muted, #909399);
}

.full-width {
  width: 100%;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 6px;
  border-radius: 8px;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: var(--admin-primary, #1ab394);
  cursor: pointer;
  transition: color 0.2s;
}

.login-link:hover {
  color: var(--admin-primary-hover, #159c81);
}

.org-input :deep(.el-input__inner) {
  cursor: pointer;
}

.org-caret {
  color: var(--admin-text-muted, #909399);
  font-size: 14px;
}

.org-tree {
  max-height: 320px;
  overflow-y: auto;
}

.org-class {
  color: var(--admin-primary, #1ab394);
  font-weight: 600;
}

.org-hint {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--admin-border, #ebeef5);
  color: var(--admin-text-muted, #909399);
  font-size: 12px;
  text-align: center;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 8px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item__error) {
  position: static;
  padding-top: 4px;
}
</style>

<!-- 下拉面板会 teleport 到 body，无法继承卡片上的主色，用唯一 popper 类单独着色 -->
<style>
.register-gender-popper .el-select-dropdown__item.is-selected {
  color: #1ab394;
  font-weight: 600;
}
</style>
