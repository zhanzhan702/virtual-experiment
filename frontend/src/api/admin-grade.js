import request from '@/utils/request'

/** 完整组织架构树（5 级嵌套） */
export function fetchOrgTree() {
  return request.get('/admin/org/tree')
}

/**
 * 某节点下所有班级的成绩汇总
 * @param {string} [orgId] 留空表示全校
 */
export function fetchClassSummaries(orgId) {
  return request.get('/admin/grades/classes', { params: { orgId } })
}

/**
 * 某节点下学生的成绩分页
 * @param {{orgId?:string,name?:string,page:number,size:number}} params
 */
export function fetchStudentGrades(params) {
  return request.get('/admin/grades/students', { params })
}

/** 某学生的完成历史（含进行中的记录） */
export function fetchStudentExperiments(userId) {
  return request.get(`/admin/grades/students/${userId}/experiments`)
}

/**
 * 人工改分
 * @param {string} experimentId
 * @param {number|null} manualScore 百分制 0-100；传 null 撤销改分
 */
export function updateExperimentScore(experimentId, manualScore) {
  return request.put(`/admin/grades/experiments/${experimentId}/score`, { manualScore })
}
