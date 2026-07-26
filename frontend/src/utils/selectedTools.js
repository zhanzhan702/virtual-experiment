import { categories } from '@/constants/tool-selection-config'
import { selectedToolsKey } from '@/constants/scene-hotspot-config'

/** 将 selectedMap { ppe: [1,5], ... } 解析为工具列表 */
export function resolveSelectedTools(selectedMap = {}) {
  const list = []
  for (const cat of categories) {
    const ids = selectedMap[cat.key] || []
    for (const id of ids) {
      const tool = cat.tools.find((t) => t.id === id)
      if (tool) {
        list.push({
          ...tool,
          categoryKey: cat.key,
          categoryTitle: cat.title
        })
      }
    }
  }
  return list
}

export function saveSelectedTools(selectedMap) {
  sessionStorage.setItem(selectedToolsKey, JSON.stringify(selectedMap || {}))
}

export function loadSelectedTools() {
  try {
    const raw = sessionStorage.getItem(selectedToolsKey)
    if (!raw) return []
    return resolveSelectedTools(JSON.parse(raw))
  } catch (_) {
    return []
  }
}
