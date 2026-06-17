import request from '@/utils/request'

/** 启动实验（按模板编码） */
export function startExperiment(templateCode) {
  return request.post('/experiment/start', { templateCode })
}

/** 提交实验步骤 */
export function submitStep(data) {
  return request.post('/experiment/step/submit', data)
}
