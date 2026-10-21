<template>
  <span class="ticket-date">
    <span
      v-for="(unit, index) in DATE_UNITS"
      :key="unit"
      class="date-part"
      :class="{ year: index === 0 }"
    >
      <span class="date-value">{{ parts[index] || (slashed ? '／' : '\u00a0') }}</span>
      <span>{{ unit }}</span>
    </span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: String, default: '' },
  slashed: { type: Boolean, default: false }
})
const DATE_UNITS = ['年', '月', '日', '时', '分']
const parts = computed(() => props.value.match(/\d+/g) || [])
</script>

<style scoped>
.ticket-date {
  display: inline-flex;
  align-items: baseline;
  width: 100%;
  gap: 0.25em;
  min-width: 0;
}
.date-part {
  display: inline-flex;
  flex: 1;
  align-items: baseline;
  gap: 0.2em;
  min-width: 0;
}
.date-part.year {
  flex: 1.35;
}
.date-value {
  flex: 1;
  min-width: 0;
  border-bottom: 1px solid currentColor;
  text-align: center;
  line-height: 1.25;
}
</style>
