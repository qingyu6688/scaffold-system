<script setup lang="ts">
import Checkbox from './Checkbox.vue'
import type { SelectOption } from './Select.vue'

const props = defineProps<{
  modelValue: number[]
  options: SelectOption[]
  invalid?: boolean
  disabled?: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', value: number[]): void }>()

function toggle(value: number, selected: boolean): void {
  const next = selected ? [...new Set([...props.modelValue, value])] : props.modelValue.filter(id => id !== value)
  emit('update:modelValue', next)
}
</script>

<template>
  <div class="multi-select" :class="{ 'is-invalid': invalid }" role="group" :aria-invalid="invalid || undefined">
    <Checkbox v-for="option in options" :key="option.value"
      :model-value="modelValue.includes(Number(option.value))"
      :label="option.label" :disabled="disabled || option.disabled"
      @update:model-value="toggle(Number(option.value), $event)" />
    <span v-if="!options.length" class="muted">暂无可选项</span>
    <Checkbox v-for="id in modelValue.filter(id => !options.some(option => Number(option.value) === id))"
      :key="`retained-${id}`" :model-value="true" :label="`已分配项 #${id}（当前不可新增）`"
      :disabled="disabled" @update:model-value="toggle(id, $event)" />
  </div>
</template>

<style scoped>
.multi-select { display: flex; flex-wrap: wrap; gap: 10px 16px; padding: 10px 12px; border: 1px solid var(--line-strong); border-radius: var(--r-xs); }
.multi-select.is-invalid { border-color: var(--red-text); }
</style>
