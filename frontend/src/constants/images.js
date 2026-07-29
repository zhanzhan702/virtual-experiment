/**
 * 图片资源统一导出
 * 新增/重命名图片只需修改此文件，组件无需改动
 *
 * 使用方式：
 *   import Images from '@/constants/images'
 *   // JS:  Images.leftFence
 *   // 模板: :src="Images.leftFence"
 */

// ─── 通用 UI ───
import banner from '@/assets/images/banner.png'
import mjuLogo from '@/assets/images/MJUlogo.png'
import confirmButton from '@/assets/images/ConfirmButton.png'
import confirmButtonHover from '@/assets/images/ConfirmButtonHover.png'

// ─── 场景选择 ───
import highVoltageButton from '@/assets/images/HighVoltageButton.png'
import highVoltageButtonHover from '@/assets/images/HighVoltageButtonHover.png'
import lowVoltageButton from '@/assets/images/LowVoltageButton.png'
import lowVoltageButtonHover from '@/assets/images/LowVoltageButtonHover.png'

// ─── 实验背景 ───
import experimentViewBg from '@/assets/images/ExperimentViewBackground.jpg'
import highWorkBg from '@/assets/images/HighWorkBackground.png'
import simulationDisclaimerNotice from '@/assets/images/SimulationDisclaimerNotice.png'

// ─── 保存/工作任务按钮 ───
import saveProgressIcon from '@/assets/images/SaveProgressIcon.png'
import saveProgressIconHover from '@/assets/images/SaveProgressIconHover.png'
import workTaskButton from '@/assets/images/WorkTaskButton.png'
import workTaskButtonHover from '@/assets/images/WorkTaskButtonHover.png'

// ─── 高压全景 ───
import distributionRoomPanorama from '@/assets/images/DistributionRoomPanorama.jpg'

// ─── 高压工作票 ───
import hwtBackground from '@/assets/images/HWTBackgroundImage.jpg'

// ─── 高压工器具选择 ───
import toolSelectionBg from '@/assets/images/ToolSelectionBackground.jpg'

// ─── 柜体局部操作 ───
import cabinetGroupOverview from '@/assets/images/CabinetGroupOverview.png'
import cabinetLocalOperation from '@/assets/images/CabinetLocalOperation.png'
import powerSocket from '@/assets/images/PowerSocket.png'
import leftFence from '@/assets/images/LeftFence.png'
import rightFence from '@/assets/images/RightFence.png'
import signStopHighVoltage from '@/assets/images/SignStopHighVoltage.png'
import signPersonWorking from '@/assets/images/SignPersonWorking.png'
import safetyNotice from '@/assets/images/safetyNotice.png'
import voltageTesterNormal from '@/assets/images/voltageTesterNormal.png'
import voltageTesterWarning from '@/assets/images/voltageTesterWarning.png'

// ─── 后续步骤（预留） ───
import checkMeterReading from '@/assets/images/CheckMeterReading.png'
import verifyMeterDataConsistency from '@/assets/images/VerifyMeterDataConsistency.png'
import verifyTerminalWiring from '@/assets/images/VerifyTerminalWiring.png'

export default {
  // 通用
  banner,
  mjuLogo,
  confirmButton,
  confirmButtonHover,
  // 场景选择
  highVoltageButton,
  highVoltageButtonHover,
  lowVoltageButton,
  lowVoltageButtonHover,
  // 实验背景
  experimentViewBg,
  highWorkBg,
  simulationDisclaimerNotice,
  // 按钮
  saveProgressIcon,
  saveProgressIconHover,
  workTaskButton,
  workTaskButtonHover,
  // 高压全景
  distributionRoomPanorama,
  // 高压工作票
  hwtBackground,
  // 高压工器具
  toolSelectionBg,
  // 柜体操作
  cabinetGroupOverview,
  cabinetLocalOperation,
  powerSocket,
  leftFence,
  rightFence,
  signStopHighVoltage,
  signPersonWorking,
  safetyNotice,
  voltageTesterNormal,
  voltageTesterWarning,
  // 后续步骤
  checkMeterReading,
  verifyMeterDataConsistency,
  verifyTerminalWiring
}

/**
 * CSS 变量映射（供 main.js 注入）
 * CSS 中用 var(--img-xxx) 引用，更换格式只改上方 import
 */
export const cssVars = {
  '--img-confirm-btn': confirmButton,
  '--img-confirm-btn-hover': confirmButtonHover,
  '--img-high-voltage-btn': highVoltageButton,
  '--img-high-voltage-btn-hover': highVoltageButtonHover,
  '--img-low-voltage-btn': lowVoltageButton,
  '--img-low-voltage-btn-hover': lowVoltageButtonHover,
  '--img-experiment-bg': experimentViewBg,
  '--img-save-icon': saveProgressIcon,
  '--img-save-icon-hover': saveProgressIconHover,
  '--img-work-task': workTaskButton,
  '--img-work-task-hover': workTaskButtonHover,
  '--img-tool-selection-bg': toolSelectionBg,
  '--img-hwt-bg': hwtBackground
}
