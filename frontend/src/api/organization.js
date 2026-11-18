import request from '@/utils/request'

/** 获取组织架构树 */
export function getOrgTree() {
  return request.get('/organization/tree')
}
