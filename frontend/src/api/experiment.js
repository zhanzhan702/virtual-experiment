import request from '@/utils/request'

/** 启动实验 */
export function startExperiment(templateId) {
  return request.post('/experiment/start', { templateId })
}

/** 提交实验步骤 */
export function submitStep(data) {
  return request.post('/experiment/step/submit', data)
}
