import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchOrgTree } from '@/api/admin-org'

/**
 * 组织架构树共享缓存。
 *
 * <p>「查看学生成绩」与「专业班级管理」各挂一个 `OrgTreePanel`，但两处的树内容完全一样。
 * 把数据放在 store 里让两个实例共读同一份之后：
 *
 * - 页面间来回切换不再重拉整棵 5 级树（这是后台里最重的一个请求）
 * - 任一处增删改后另一处自动看到新数据，不必各自 refresh
 *
 * <p>注意 `OrgTreePanel` 里 `tree` 必须是 `computed(() => store.tree)` 而不是本地 `ref`，
 * 否则实例会各持一份快照，共享就失效了。
 */
export const useOrgTreeStore = defineStore('orgTree', () => {
  const tree = ref([])
  const loading = ref(false)

  /** 进行中的请求：两处 ensure() 几乎同时调用时共用一个 Promise，不会发出两个请求 */
  let inflight = null

  /**
   * 取树数据；命中缓存直接返回，不发请求。
   *
   * @param {boolean} force 忽略缓存强制重拉（增删改后、用户点刷新时用）
   * @returns {Promise<Array>} 树数据
   */
  async function ensure(force = false) {
    if (tree.value.length && !force) {
      return tree.value
    }
    if (inflight) {
      return inflight
    }

    loading.value = true
    inflight = fetchOrgTree()
      .then(data => {
        tree.value = data || []
        return tree.value
      })
      .finally(() => {
        loading.value = false
        inflight = null
      })
    return inflight
  }

  return { tree, loading, ensure }
})
