import request from '@/utils/request'

/**
 * 分页查询可见用户
 * @param {{page:number,size:number,name?:string,username?:string,phone?:string,orgId?:string}} params
 * @returns {Promise<{records:Array,total:number,page:number,size:number}>}
 */
export function fetchUserPage(params) {
  return request.get('/admin/users', { params })
}

/** 用户详情（含高/低压实验记录） */
export function fetchUserDetail(id) {
  return request.get(`/admin/users/${id}`)
}

/** 重置他人密码（不改动对方已签发的 token，下次登录生效） */
export function resetUserPassword(id, data) {
  return request.put(`/admin/users/${id}/password`, data)
}
