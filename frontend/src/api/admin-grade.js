import request from '@/utils/request'

/*
 * 组织架构树接口不在这里 —— /admin/org/tree 已迁到 AdminOrgController，
 * 前端对应 admin-org.js 的 fetchOrgTree，并由 stores/orgTree.js 统一缓存。
 */

/**
 * 某节点下所有班级的成绩汇总
 * @param {string} [orgId] 留空表示全校
 */
export function fetchClassSummaries(orgId) {
  return request.get('/admin/grades/classes', { params: { orgId } })
}

/**
 * 单个班级的成绩概况：完成率、平均/最高/最低分、分数段分布、未完成名单
 *
 * 分数口径与 fetchClassSummaries 的 avgScore 一致（高压分与低压分混在同一批里统计），
 * 因此同一个班在两处的平均分必然相同。
 *
 * @param {string} orgId 必须是班级节点，传年级/学院会 404
 */
export function fetchClassOverview(orgId) {
  return request.get(`/admin/grades/classes/${orgId}/overview`)
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
