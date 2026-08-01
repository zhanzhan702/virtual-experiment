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
import electrifyCompleteNotice from '@/assets/images/ElectrifyCompleteNotice.png'

// ─── 柜体操作 — 右侧工具栏（终端/工器具/线材） ───
import threePhaseThreeWireMeter from '@/assets/images/ThreePhaseThreeWireMeter.png'
import threePhaseThreeWireTerminal from '@/assets/images/ThreePhaseThreeWireTerminal.png'
import crossScrewdriver from '@/assets/images/CrossScrewdriver.png'
import wireStripper from '@/assets/images/WireStripper.png'
import seal from '@/assets/images/Seal.png'
import wire25mm2Yellow from '@/assets/images/Wire25mm2Yellow.png'
import wire4mm2Yellow from '@/assets/images/Wire4mm2Yellow.png'
import wire4mm2YellowBlack from '@/assets/images/Wire4mm2YellowBlack.png'
import wire25mm2Green from '@/assets/images/Wire25mm2Green.png'
import wire25mm2Red from '@/assets/images/Wire25mm2Red.png'
import wire4mm2Red from '@/assets/images/Wire4mm2Red.png'
import wire4mm2RedBlack from '@/assets/images/Wire4mm2RedBlack.png'
import cableTieLabel from '@/assets/images/CableTieLabel.png'
import remoteControlCable2Core from '@/assets/images/RemoteControlCable2Core.png'
import remoteSignalCable2Core from '@/assets/images/RemoteSignalCable2Core.png'
import signalCable6Core from '@/assets/images/SignalCable6Core.png'
import signalCable8Core from '@/assets/images/SignalCable8Core.png'
import communicationModule from '@/assets/images/CommunicationModule.png'
import simCard from '@/assets/images/SimCard.png'
import antenna from '@/assets/images/Antenna.png'

// ─── 计量小室（leafer ui 画布） ───
import meteringRoomNoMeter from '@/assets/images/MeteringRoomNoMeter.png'
import meteringRoomWithMeter from '@/assets/images/MeteringRoomWithMeter.png'
import meteringRoomWired from '@/assets/images/MeteringRoomWired.png'
import meteringRoomWithCableTies from '@/assets/images/MeteringRoomWithCableTies.png'
import junctionBox from '@/assets/images/JunctionBox.png'
import junctionBoxSwitch from '@/assets/images/JunctionBoxSwitch.png'
import strippedSignalCable6Core from '@/assets/images/StrippedSignalCable6Core.png'

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
  electrifyCompleteNotice,
  // 柜体操作 — 右侧工具栏
  threePhaseThreeWireMeter,
  threePhaseThreeWireTerminal,
  crossScrewdriver,
  wireStripper,
  seal,
  wire25mm2Yellow,
  wire4mm2Yellow,
  wire4mm2YellowBlack,
  wire25mm2Green,
  wire25mm2Red,
  wire4mm2Red,
  wire4mm2RedBlack,
  cableTieLabel,
  remoteControlCable2Core,
  remoteSignalCable2Core,
  signalCable6Core,
  signalCable8Core,
  communicationModule,
  simCard,
  antenna,
  // 计量小室（leafer ui 画布）
  meteringRoomNoMeter,
  meteringRoomWithMeter,
  meteringRoomWired,
  meteringRoomWithCableTies,
  junctionBox,
  junctionBoxSwitch,
  strippedSignalCable6Core,
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
