/**
 * 图片资源统一导出
 * 新增/重命名图片只需修改此文件，组件无需改动
 *
 * 使用方式：
 *   import Images from '@/assets/images'
 *   // JS:  Images.leftFence
 *   // 模板: :src="Images.leftFence"
 */

// ─── 通用 UI ───
import banner from './banner.png'
import mjuLogo from './MJUlogo.png'
import confirmButton from './ConfirmButton.png'
import confirmButtonHover from './ConfirmButtonHover.png'

// ─── 场景选择 ───
import highVoltageButton from './HighVoltageButton.png'
import highVoltageButtonHover from './HighVoltageButtonHover.png'
import lowVoltageButton from './LowVoltageButton.png'
import lowVoltageButtonHover from './LowVoltageButtonHover.png'

// ─── 实验背景 ───
import experimentViewBg from './ExperimentViewBackground.jpg'
import highWorkBg from './HighWorkBackground.png'
import simulationDisclaimerNotice from './SimulationDisclaimerNotice.png'

// ─── 保存/工作任务按钮 ───
import saveProgressIcon from './SaveProgressIcon.png'
import saveProgressIconHover from './SaveProgressIconHover.png'
import workTaskButton from './WorkTaskButton.png'
import workTaskButtonHover from './WorkTaskButtonHover.png'

// ─── 高压全景 ───
import distributionRoomPanorama from './DistributionRoomPanorama.jpg'

// ─── 高压工作票 ───
import hwtBackground from './HWTBackgroundImage.jpg'

// ─── 高压工器具选择 ───
import toolSelectionBg from './ToolSelectionBackground.jpg'

// ─── 柜体局部操作 ───
import cabinetGroupOverview from './CabinetGroupOverview.png'
import cabinetLocalOperation from './CabinetLocalOperation.png'
import powerSocket from './PowerSocket.png'
import leftFence from './LeftFence.png'
import rightFence from './RightFence.png'
import signStopHighVoltage from './SignStopHighVoltage.png'
import signPersonWorking from './SignPersonWorking.png'
import safetyNotice from './safetyNotice.png'
import voltageTesterNormal from './voltageTesterNormal.png'
import voltageTesterWarning from './voltageTesterWarning.png'

// ─── 后续步骤（预留） ───
import checkMeterReading from './CheckMeterReading.png'
import verifyMeterDataConsistency from './VerifyMeterDataConsistency.png'
import verifyTerminalWiring from './VerifyTerminalWiring.png'

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
  verifyTerminalWiring,
}
