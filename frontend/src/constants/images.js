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

// ─── 工器具选择 — 个人防护 ───
import blueSafetyHelmet from '@/assets/images/BlueSafetyHelmet.png'
import cottonGloves from '@/assets/images/CottonGloves.png'
import electricalInsulatingShoes from '@/assets/images/ElectricalInsulatingShoes.png'
import leatherShoes from '@/assets/images/LeatherShoes.png'
import motorcycleHelmet from '@/assets/images/MotorcycleHelmet.png'
import redSafetyHelmet from '@/assets/images/RedSafetyHelmet.png'
import rubberGloves from '@/assets/images/RubberGloves.png'
import safetyGoggles from '@/assets/images/SafetyGoggles.png'
import sandal from '@/assets/images/Sandal.png'
import tShirtAndShorts from '@/assets/images/TShirtAndShorts.png'
import workClothes from '@/assets/images/WorkClothes.png'
import workClothesTopAndJeans from '@/assets/images/WorkClothesTopAndJeans.png'

// ─── 工器具选择 — 终端 ───
import terminalImg1 from '@/assets/images/2/1.png'
import terminalImg2 from '@/assets/images/2/2.png'
import terminalImg3 from '@/assets/images/2/3.png'
import terminalImg4 from '@/assets/images/2/4.png'
import terminalImg5 from '@/assets/images/2/5.png'

// ─── 工器具选择 — 工器具 ───
import toolImg1 from '@/assets/images/3/1.png'
import toolImg2 from '@/assets/images/3/2.png'
import toolImg3 from '@/assets/images/3/3.png'
import toolImg4 from '@/assets/images/3/4.png'
import toolImg5 from '@/assets/images/3/5.png'
import toolImg6 from '@/assets/images/3/6.png'
import toolImg7 from '@/assets/images/3/7.png'
import toolImg8 from '@/assets/images/3/8.png'

// ─── 柜体局部操作 ───
import cabinetGroupOverview from '@/assets/images/CabinetGroupOverview.png'
import cabinetLocalOperation from '@/assets/images/CabinetLocalOperation.png'
import powerSocket from '@/assets/images/PowerSocket.png'
import leftFence from '@/assets/images/LeftFence.png'
import rightFence from '@/assets/images/RightFence.png'
import signStopHighVoltage from '@/assets/images/SignStopHighVoltage.png'
import signPersonWorking from '@/assets/images/SignPersonWorking.png'
import safetyNotice from '@/assets/images/SafetyNotice.png'
import voltageTesterNormal from '@/assets/images/VoltageTesterNormal.png'
import voltageTesterWarning from '@/assets/images/VoltageTesterWarning.png'

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
  // 工器具选择
  // 1.个人防护
  blueSafetyHelmet,
  cottonGloves,
  electricalInsulatingShoes,
  leatherShoes,
  motorcycleHelmet,
  redSafetyHelmet,
  rubberGloves,
  safetyGoggles,
  sandal,
  tShirtAndShorts,
  workClothes,
  workClothesTopAndJeans,
  // 2.终端
  terminalImg1,
  terminalImg2,
  terminalImg3,
  terminalImg4,
  terminalImg5,
  // 3.工器具
  toolImg1,
  toolImg2,
  toolImg3,
  toolImg4,
  toolImg5,
  toolImg6,
  toolImg7,
  toolImg8,
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
