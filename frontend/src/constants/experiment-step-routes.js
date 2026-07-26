/**
 * 高压实验 stepOrder → 路由
 * A1–A3 为工器具选择之后的前端子流程（方案 B，暂不单独占库表步骤）
 */
export const highVoltageStepRoutes = {
  1: '/HWT',
  2: '/HTS',
  // 步骤 2 完成后进入配电室总览；恢复实验时若 next 为 3，也先进入总览
  3: '/HScene'
}

export function getHighVoltageStepPath(stepOrder) {
  return highVoltageStepRoutes[stepOrder] || '/HWT'
}
