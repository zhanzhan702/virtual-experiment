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
import banner from '@/assets/images/common/banner.png'
import mjuLogo from '@/assets/images/common/MJUlogo.png'
import confirmButton from '@/assets/images/common/ConfirmButton.png'
import confirmButtonHover from '@/assets/images/common/ConfirmButtonHover.png'

// ─── 场景选择 ───
import highVoltageButton from '@/assets/images/scenario/HighVoltageButton.png'
import highVoltageButtonHover from '@/assets/images/scenario/HighVoltageButtonHover.png'
import lowVoltageButton from '@/assets/images/scenario/LowVoltageButton.png'
import lowVoltageButtonHover from '@/assets/images/scenario/LowVoltageButtonHover.png'

// ─── 实验背景 ───
import experimentViewBg from '@/assets/images/scenario/ExperimentViewBackground.jpg'
import highWorkBg from '@/assets/images/scene/HighWorkBackground.png'
import simulationDisclaimerNotice from '@/assets/images/scenario/SimulationDisclaimerNotice.png'

// ─── 保存/工作任务按钮 ───
import saveProgressIcon from '@/assets/images/common/SaveProgressIcon.png'
import saveProgressIconHover from '@/assets/images/common/SaveProgressIconHover.png'
import workTaskButton from '@/assets/images/common/WorkTaskButton.png'
import workTaskButtonHover from '@/assets/images/common/WorkTaskButtonHover.png'

// ─── 高压全景 ───
import distributionRoomPanorama from '@/assets/images/scene/DistributionRoomPanorama.jpg'

// ─── 高压工作票 ───
import hwtBackground from '@/assets/images/scene/HWTBackgroundImage.jpg'

// ─── 高压工器具选择 ───
import toolSelectionBg from '@/assets/images/scene/ToolSelectionBackground.jpg'

// ─── 工器具选择 — 个人防护 ───
import blueSafetyHelmet from '@/assets/images/选择工器具/BlueSafetyHelmet.png'
import cottonGloves from '@/assets/images/选择工器具/CottonGloves.png'
import electricalInsulatingShoes from '@/assets/images/选择工器具/ElectricalInsulatingShoes.png'
import leatherShoes from '@/assets/images/选择工器具/LeatherShoes.png'
import motorcycleHelmet from '@/assets/images/选择工器具/MotorcycleHelmet.png'
import redSafetyHelmet from '@/assets/images/选择工器具/RedSafetyHelmet.png'
import rubberGloves from '@/assets/images/选择工器具/RubberGloves.png'
import safetyGoggles from '@/assets/images/选择工器具/SafetyGoggles.png'
import sandal from '@/assets/images/选择工器具/Sandal.png'
import tShirtAndShorts from '@/assets/images/选择工器具/TShirtAndShorts.png'
import workClothes from '@/assets/images/选择工器具/WorkClothes.png'
import workClothesTopAndJeans from '@/assets/images/选择工器具/WorkClothesTopAndJeans.png'

// ─── 柜体局部操作 ───
import cabinetGroupOverview from '@/assets/images/道具栏/CabinetGroupOverview.png'
import powerSocket from '@/assets/images/道具栏/PowerSocket.png'
import leftFence from '@/assets/images/道具栏/LeftFence.png'
import rightFence from '@/assets/images/道具栏/RightFence.png'
import signStopHighVoltage from '@/assets/images/道具栏/SignStopHighVoltage.png'
import signPersonWorking from '@/assets/images/道具栏/SignPersonWorking.png'
import safetyNotice from '@/assets/images/道具栏/SafetyNotice.png'
import voltageTesterNormal from '@/assets/images/道具栏/VoltageTesterNormal.png'
import voltageTesterWarning from '@/assets/images/道具栏/VoltageTesterWarning.png'
import electrifyCompleteNotice from '@/assets/images/道具栏/ElectrifyCompleteNotice.png'

// ─── 柜体操作 — 右侧工具栏（终端/工器具/线材） ───
import threePhaseThreeWireMeter from '@/assets/images/道具栏/ThreePhaseThreeWireMeter.png'
import threePhaseThreeWireTerminal from '@/assets/images/道具栏/ThreePhaseThreeWireTerminal.png'
import crossScrewdriver from '@/assets/images/道具栏/CrossScrewdriver.png'
import wireStripper from '@/assets/images/道具栏/WireStripper.png'
import seal from '@/assets/images/道具栏/Seal.png'
import wire25mm2Yellow from '@/assets/images/道具栏/Wire25mm2Yellow.png'
import wire4mm2Yellow from '@/assets/images/道具栏/Wire4mm2Yellow.png'
import wire4mm2YellowBlack from '@/assets/images/道具栏/Wire4mm2YellowBlack.png'
import wire25mm2Green from '@/assets/images/道具栏/Wire25mm2Green.png'
import wire25mm2Red from '@/assets/images/道具栏/Wire25mm2Red.png'
import wire4mm2Red from '@/assets/images/道具栏/Wire4mm2Red.png'
import wire4mm2RedBlack from '@/assets/images/道具栏/Wire4mm2RedBlack.png'
import cableTieLabel from '@/assets/images/道具栏/CableTieLabel.png'
import remoteControlCable2Core from '@/assets/images/道具栏/RemoteControlCable2Core.png'
import remoteSignalCable2Core from '@/assets/images/道具栏/RemoteSignalCable2Core.png'
import signalCable6Core from '@/assets/images/道具栏/SignalCable6Core.png'
import signalCable8Core from '@/assets/images/道具栏/SignalCable8Core.png'
import communicationModule from '@/assets/images/道具栏/CommunicationModule.png'
import simCard from '@/assets/images/道具栏/SimCard.png'
import antenna from '@/assets/images/道具栏/Antenna.png'

// ─── 计量小室（leafer ui 画布） ───
import meteringRoomNoMeter from '@/assets/images/metering-room/MeteringRoomNoMeter.png'
import meteringRoomWithMeter from '@/assets/images/metering-room/MeteringRoomWithMeter.png'
import meteringRoomWired from '@/assets/images/metering-room/MeteringRoomWired.png'
import meteringRoomWithCableTies from '@/assets/images/metering-room/MeteringRoomWithCableTies.png'
import junctionBox from '@/assets/images/metering-room/JunctionBox.png'
import junctionBoxSwitch from '@/assets/images/metering-room/JunctionBoxSwitch.png'
import strippedSignalCable6Core from '@/assets/images/metering-room/StrippedSignalCable6Core.png'

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
