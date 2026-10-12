<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import Icon from '@/components/Icon.vue'
import { Avatar, Button, EmptyState, PageHeader, SectionLabel, SpecPlate, Tag, TextInput } from '@/components/ui'
import PasswordDialog from '@/components/layout/PasswordDialog.vue'
import { authApi, type Profile } from '@/api'
import { authed, refreshUser } from '@/composables/auth'
import { confirm } from '@/composables/confirm'
import { pushToast, toastError } from '@/composables/toast'
import { floatBarEnter, floatBarLeave, revealCascade, settlePop } from '@/composables/motion'
import { email, maxLength, phone, required, validate } from '@/utils/validate'

/* ============================================================
   个人中心：沿用参数「设置面板」版式。
   本人只能改姓名与联系方式；账号、部门、角色、岗位由管理员维护，只读展示。
   ============================================================ */

type EditableKey = 'nickname' | 'phone' | 'email'
interface EditableField {
  key: EditableKey
  title: string
  remark: string
  type: 'text' | 'tel' | 'email'
  autocomplete: string
  maxlength: number
  placeholder?: string
}

const FIELDS: EditableField[] = [
  { key: 'nickname', title: '姓名', remark: '显示在顶栏、操作日志和人员选择中', type: 'text', autocomplete: 'name', maxlength: 64 },
  { key: 'phone', title: '手机号码', remark: '方便同事与管理员联系，可留空', type: 'tel', autocomplete: 'tel', maxlength: 32, placeholder: '选填' },
  { key: 'email', title: '电子邮箱', remark: '工作邮箱，可留空', type: 'email', autocomplete: 'email', maxlength: 128, placeholder: '选填' },
]
const SECTIONS = [
  { id: 'profile-basic', name: '基本资料' },
  { id: 'profile-org', name: '组织与角色' },
  { id: 'profile-security', name: '账号安全' },
]

const profile = ref<Profile | null>(null)
const loading = ref(false)
const loadFailed = ref(false)
const saving = ref(false)
const passwordOpen = ref(false)
const body = ref<HTMLElement | null>(null)

/** saved 为服务端最新值，form 为草稿；两者不同即为未保存改动 */
const saved = reactive<Record<EditableKey, string>>({ nickname: '', phone: '', email: '' })
const form = reactive<Record<EditableKey, string>>({ nickname: '', phone: '', email: '' })
const errors = ref<Partial<Record<EditableKey, string>>>({})

function applyProfile(data: Profile) {
  profile.value = data
  const next = { nickname: data.nickname ?? '', phone: data.phone ?? '', email: data.email ?? '' }
  Object.assign(saved, next)
  Object.assign(form, next)
  errors.value = {}
}

async function load() {
  loading.value = true
  loadFailed.value = false
  try {
    applyProfile(await authApi.profile())
    await nextTick()
    revealCascade(body.value, '.settings-group', 0.08)
  } catch (error) {
    loadFailed.value = true
    toastError(error, '个人资料加载失败')
  } finally {
    loading.value = false
  }
}
onMounted(load)

const dirtyKeys = computed(() => FIELDS.map(f => f.key).filter(key => form[key].trim() !== saved[key]))
const dirtyCount = computed(() => dirtyKeys.value.length)

function setValue(key: EditableKey, value: string | number | null) {
  form[key] = value === null ? '' : String(value)
  if (errors.value[key]) errors.value = { ...errors.value, [key]: undefined }
}

function undo(key: EditableKey) {
  setValue(key, saved[key])
}

function discard() {
  Object.assign(form, saved)
  errors.value = {}
}

async function submit() {
  errors.value = validate(form, {
    nickname: [required('请输入姓名'), maxLength(64)],
    phone: [phone],
    email: [email, maxLength(128)],
  })
  if (Object.values(errors.value).some(Boolean)) {
    pushToast('请先修正标红的字段', 'danger')
    return
  }
  const changed = [...dirtyKeys.value]
  saving.value = true
  try {
    const data = await authApi.updateProfile({ nickname: form.nickname.trim(), phone: form.phone.trim(), email: form.email.trim() })
    applyProfile(data)
    pushToast('个人资料已保存')
    nextTick(() => changed.forEach(key => settlePop(document.querySelector(`[data-field="${key}"] .settings-control`))))
    // 顶栏与菜单中的姓名以 /auth/me 为准，保存后同步一次
    await refreshUser().catch((error: unknown) => toastError(error, '顶栏信息刷新失败，重新登录后生效'))
  } catch (error) {
    toastError(error, '个人资料保存失败')
  } finally {
    saving.value = false
  }
}

function jump(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const specRows = computed(() => [
  { label: '登录账号', value: profile.value?.username },
  { label: '创建时间', value: profile.value?.createTime },
  { label: '最近登录', value: profile.value?.lastLoginTime },
])

// 会话已清理（退出、改密）时直接放行，不再追问未保存改动
onBeforeRouteLeave(async () => {
  if (!authed.value || !dirtyCount.value) return true
  return confirm({ title: '放弃未保存的改动', message: '个人资料还有未保存的改动，离开页面将放弃这些改动。', confirmText: '放弃改动', danger: true })
})
</script>

<template>
  <div class="page">
    <PageHeader title="个人中心" desc="维护自己的姓名与联系方式，查看所属组织、角色和最近的登录记录。">
      <template #spec><SpecPlate title="账号" :rows="specRows" /></template>
    </PageHeader>

    <div v-if="loadFailed && !profile" class="m-surface panel reveal" style="padding: 48px 16px">
      <EmptyState title="个人资料加载失败" desc="请检查网络后重试。" icon="alert" />
      <div style="display: flex; justify-content: center; margin-top: 12px">
        <Button icon="refresh" :loading="loading" @click="load">重新加载</Button>
      </div>
    </div>

    <div v-else class="settings">
      <aside class="settings-nav m-surface">
        <div class="profile-id">
          <Avatar :name="saved.nickname || profile?.username" :size="56" />
          <div class="profile-id-name ellipsis">{{ saved.nickname || '—' }}</div>
          <div class="profile-id-sub mono ellipsis">@{{ profile?.username ?? '—' }}</div>
          <div class="profile-id-sub ellipsis">{{ profile?.deptName || '未分配部门' }}</div>
        </div>
        <nav aria-label="资料分组">
          <button v-for="(s, i) in SECTIONS" :key="s.id" type="button" class="settings-nav-item" @click="jump(s.id)">
            <span class="mono faint">{{ String(i + 1).padStart(2, '0') }}</span>{{ s.name }}
          </button>
        </nav>
        <p class="settings-nav-note faint"><Icon name="lock" :size="12" />部门、角色和岗位由管理员维护</p>
      </aside>

      <div ref="body" class="settings-body">
        <p v-if="loading && !profile" class="band-empty faint">正在加载个人资料…</p>
        <template v-else-if="profile">
          <section id="profile-basic" class="settings-group m-surface">
            <SectionLabel title="基本资料" as="h3" />
            <div class="settings-row is-locked">
              <div class="settings-label">
                <span class="settings-title">登录账号<Icon name="lock" :size="12" class="faint" /></span>
                <span class="settings-remark">登录时使用，创建后不能修改</span>
              </div>
              <div class="settings-control"><span class="settings-readonly mono">{{ profile.username }}</span></div>
            </div>
            <div
              v-for="f in FIELDS"
              :key="f.key"
              :data-field="f.key"
              :class="['settings-row', { 'is-dirty': dirtyKeys.includes(f.key), 'is-failed': !!errors[f.key] }]"
            >
              <div class="settings-label">
                <label class="settings-title" :for="`profile-${f.key}`">{{ f.title }}<span v-if="f.key === 'nickname'" class="faint" aria-hidden="true">*</span></label>
                <span class="settings-remark">{{ f.remark }}</span>
              </div>
              <div class="settings-control">
                <TextInput
                  :input-id="`profile-${f.key}`"
                  :model-value="form[f.key]"
                  :type="f.type"
                  :autocomplete="f.autocomplete"
                  :maxlength="f.maxlength"
                  :placeholder="f.placeholder"
                  :invalid="!!errors[f.key]"
                  @update:model-value="setValue(f.key, $event)"
                />
              </div>
              <div v-if="errors[f.key]" class="settings-error" role="alert">{{ errors[f.key] }}</div>
              <div v-else-if="dirtyKeys.includes(f.key)" class="settings-was">
                <span>原值 <s>{{ saved[f.key] || '（空）' }}</s></span>
                <button type="button" class="settings-undo" @click="undo(f.key)">撤销</button>
              </div>
            </div>
          </section>

          <section id="profile-org" class="settings-group m-surface">
            <SectionLabel title="组织与角色" as="h3" />
            <div class="settings-row is-locked">
              <div class="settings-label">
                <span class="settings-title">所属部门</span>
                <span class="settings-remark">决定你在数据范围内能看到哪些数据</span>
              </div>
              <div class="settings-control"><span class="settings-readonly">{{ profile.deptName || '未分配部门' }}</span></div>
            </div>
            <div class="settings-row is-locked">
              <div class="settings-label">
                <span class="settings-title">角色</span>
                <span class="settings-remark">决定可见菜单与可执行操作，仅统计已启用的角色</span>
              </div>
              <div class="settings-control profile-tags">
                <Tag v-for="r in profile.roles" :key="r" tone="info">{{ r }}</Tag>
                <span v-if="!profile.roles.length" class="faint">暂未分配</span>
              </div>
            </div>
            <div class="settings-row is-locked">
              <div class="settings-label">
                <span class="settings-title">岗位</span>
                <span class="settings-remark">在组织中的职务</span>
              </div>
              <div class="settings-control profile-tags">
                <Tag v-for="p in profile.posts" :key="p">{{ p }}</Tag>
                <span v-if="!profile.posts.length" class="faint">暂未分配</span>
              </div>
            </div>
          </section>

          <section id="profile-security" class="settings-group m-surface">
            <SectionLabel title="账号安全" as="h3" />
            <div class="settings-row">
              <div class="settings-label">
                <span class="settings-title">登录密码</span>
                <span class="settings-remark">修改后所有已登录的设备都需要重新登录</span>
              </div>
              <div class="settings-control">
                <Button icon="key" size="sm" @click="passwordOpen = true">修改密码</Button>
              </div>
            </div>
            <div class="settings-row profile-logins-head">
              <div class="settings-label">
                <span class="settings-title">最近登录记录</span>
                <span class="settings-remark">本账号最近 {{ profile.recentLogins.length || 0 }} 次登录与退出；发现异常请尽快修改密码</span>
              </div>
            </div>
            <div v-for="(l, i) in profile.recentLogins" :key="i" class="settings-row profile-login">
              <div class="settings-label">
                <span class="mono tnum">{{ l.time }}</span>
                <span v-if="l.msg" class="settings-remark">{{ l.msg }}</span>
              </div>
              <div class="settings-control">
                <Tag :tone="l.status === 1 ? 'ok' : 'danger'" dot>{{ l.status === 1 ? '成功' : '失败' }}</Tag>
              </div>
            </div>
            <p v-if="!profile.recentLogins.length" class="band-empty faint">暂无登录记录</p>
          </section>
        </template>
      </div>
    </div>

    <Teleport to="body">
      <Transition :css="false" @enter="floatBarEnter" @leave="floatBarLeave">
        <div v-if="dirtyCount" class="float-bar" role="region" aria-label="未保存的个人资料改动">
          <span class="float-bar-count"><b class="tnum">{{ dirtyCount }}</b> 项未保存</span>
          <span class="float-bar-sep" aria-hidden="true" />
          <Button variant="text" size="sm" :disabled="saving" @click="discard">放弃</Button>
          <Button variant="primary" size="sm" :loading="saving" @click="submit">保存资料</Button>
        </div>
      </Transition>
    </Teleport>

    <PasswordDialog :open="passwordOpen" @close="passwordOpen = false" />
  </div>
</template>
