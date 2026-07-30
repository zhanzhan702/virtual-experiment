import request from '@/utils/request'

/** 启动实验（按模板编码） */
export function startExperiment(templateCode) {
  return request.post('/experiment/start', { templateCode })
}

/** 提交实验步骤 */
export function submitStep(data) {
  return request.post('/experiment/step/submit', data)
}

/** 保存步骤草稿 */
export function saveDraft(data) {
  return request.post('/experiment/step/draft', data)
}

/** 查询未完成实验列表 */
export function getUnfinishedExperiments() {
  return request.get('/experiment/unfinished')
}

/** 删除实验（级联删除步骤） */
export function deleteExperiment(experimentId) {
  return request.delete(`/experiment/${experimentId}`)
}

/** 获取步骤草稿数据 */
export function getStepDraft(experimentId, stepId) {
  return request.get('/experiment/step/draft', { params: { experimentId, stepId } })
}

/** 获取实验总耗时（所有步骤累加，含草稿） */
export function getTotalDuration(experimentId) {
  return request.get(`/experiment/${experimentId}/duration`)
}
