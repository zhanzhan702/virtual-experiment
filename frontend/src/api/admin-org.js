import request from '@/utils/request'

/** 完整组织架构树（5 级嵌套，含各节点挂靠用户数） */
export function fetchOrgTree() {
  return request.get('/admin/org/tree')
}

/**
 * 新增节点
 * @param {{parentId?:string, name:string}} data parentId 为空表示新增根节点（学校）
 */
export function createOrgNode(data) {
  return request.post('/admin/org/nodes', data)
}

/** 重命名节点（子孙 path 会由后端级联更新） */
export function renameOrgNode(id, name) {
  return request.put(`/admin/org/nodes/${id}`, { name })
}

/** 删除节点；有子节点或有用户挂靠时后端会拒绝并返回原因 */
export function deleteOrgNode(id) {
  return request.delete(`/admin/org/nodes/${id}`)
}

/**
 * 与相邻兄弟交换排序
 * @param {string} id
 * @param {'UP'|'DOWN'} direction
 */
export function moveOrgNode(id, direction) {
  return request.post(`/admin/org/nodes/${id}/move`, { direction })
}
