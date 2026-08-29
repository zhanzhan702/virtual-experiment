/**
 * 图片资源统一导出
 * 新增/重命名图片只需修改此文件，组件无需改动
 *
 * 使用方式：
 *   import Images from '@/constants/images'
 *   // JS:  Images.barLeftFence
 *   // 模板: :src="Images.barLeftFence"
 */

// ─── 通用 UI ───
import banner from '@/assets/images/common/banner.png'
import mjuLogo from '@/assets/images/common/MJUlogo.png'
import confirmButtonYellow from '@/assets/images/common/ConfirmButtonYellow.png'
import confirmButtonYellowHover from '@/assets/images/common/ConfirmButtonYellowHover.png'
import confirmButtonGreen from '@/assets/images/common/ConfirmButtonGreen.png'
import confirmButtonGreenHover from '@/assets/images/common/ConfirmButtonGreenHover.png'

// ─── 场景选择 ───
import highVoltageButton from '@/assets/images/scenario/HighVoltageButton.png'
import highVoltageButtonHover from '@/assets/images/scenario/HighVoltageButtonHover.png'
import lowVoltageButton from '@/assets/images/scenario/LowVoltageButton.png'
import lowVoltageButtonHover from '@/assets/images/scenario/LowVoltageButtonHover.png'

// ─── 实验背景 ───
import experimentViewBg from '@/assets/images/scenario/ExperimentViewBackground.jpg'
import highWorkBg from '@/assets/images/common/HighWorkBackground.png'
import simulationDisclaimerNotice from '@/assets/images/scenario/SimulationDisclaimerNotice.png'

// ─── 保存/工作任务按钮 ───
import saveProgressIcon from '@/assets/images/common/SaveProgressIcon.png'
import saveProgressIconHover from '@/assets/images/common/SaveProgressIconHover.png'
import workTaskButton from '@/assets/images/common/WorkTaskButton.png'
import workTaskButtonHover from '@/assets/images/common/WorkTaskButtonHover.png'

// ─── 高压全景 ───
import distributionRoomPanorama from '@/assets/images/cabinet/DistributionRoomPanorama.jpg'

// ─── 高压工作票 ───
import workTicketBackground from '@/assets/images/work-ticket/WorkTicketBackground.jpg'

// ─── 高压工器具选择 ───
import toolSelectionBg from '@/assets/images/tool-selection/ToolSelectionBackground.jpg'

// ─── 工器具选择 — 个人防护 ───
import blueSafetyHelmet from '@/assets/images/tool-selection/ppe/BlueSafetyHelmet.png'
import cottonGloves from '@/assets/images/tool-selection/ppe/CottonGloves.png'
import electricalInsulatingShoes from '@/assets/images/tool-selection/ppe/ElectricalInsulatingShoes.png'
import leatherShoes from '@/assets/images/tool-selection/ppe/LeatherShoes.png'
import motorcycleHelmet from '@/assets/images/tool-selection/ppe/MotorcycleHelmet.png'
import redSafetyHelmet from '@/assets/images/tool-selection/ppe/RedSafetyHelmet.png'
import rubberGloves from '@/assets/images/tool-selection/ppe/RubberGloves.png'
import safetyGoggles from '@/assets/images/tool-selection/ppe/SafetyGoggles.png'
import sandal from '@/assets/images/tool-selection/ppe/Sandal.png'
import tShirtAndShorts from '@/assets/images/tool-selection/ppe/TShirtAndShorts.png'
import workClothes from '@/assets/images/tool-selection/ppe/WorkClothes.png'
import workClothesTopAndJeans from '@/assets/images/tool-selection/ppe/WorkClothesTopAndJeans.png'

// ─── 工器具选择 — 终端（选择页默认无标记） ───
import threePhaseThreeWireTerminal from '@/assets/images/tool-selection/terminal/ThreePhaseThreeWireTerminal.png'
import threePhaseFourWireTerminal from '@/assets/images/tool-selection/terminal/ThreePhaseFourWireTerminal.png'
import terminal230M from '@/assets/images/tool-selection/terminal/Terminal230M.png'
import concentrator from '@/assets/images/tool-selection/terminal/Concentrator.png'
import collector from '@/assets/images/tool-selection/terminal/Collector.png'

// ─── 工器具选择 — 工器具 ───
import flatheadScrewdriver from '@/assets/images/tool-selection/tools-and-equipment/FlatheadScrewdriver.png'
import crossScrewdriver from '@/assets/images/tool-selection/tools-and-equipment/CrossScrewdriver.png'
import adjustableWrench from '@/assets/images/tool-selection/tools-and-equipment/AdjustableWrench.png'
import needleNosePliers from '@/assets/images/tool-selection/tools-and-equipment/NeedleNosePliers.png'
import slipJointPliers from '@/assets/images/tool-selection/tools-and-equipment/SlipJointPliers.png'
import wireStripper from '@/assets/images/tool-selection/tools-and-equipment/WireStripper.png'
import voltageTester from '@/assets/images/tool-selection/tools-and-equipment/VoltageTester.png'
import seal from '@/assets/images/tool-selection/tools-and-equipment/Seal.png'

// ─── 工器具选择 — 线材（选择页 3:5 版；黑色线仅 25mm 规格） ───
import wireGreen from '@/assets/images/tool-selection/wire/WireGreen.png'
import wireRed from '@/assets/images/tool-selection/wire/WireRed.png'
import wireRedBlack from '@/assets/images/tool-selection/wire/WireRedBlack.png'
import wireYellow from '@/assets/images/tool-selection/wire/WireYellow.png'
import wireYellowBlack from '@/assets/images/tool-selection/wire/WireYellowBlack.png'
import wire25mmBlack from '@/assets/images/tool-selection/wire/Wire25mmBlack.png'
import wire2Core from '@/assets/images/tool-selection/wire/Wire2Core.png'
import signalCable6Core from '@/assets/images/tool-selection/wire/SignalCable6Core.png'
import signalCable8Core from '@/assets/images/tool-selection/wire/SignalCable8Core.png'
import communicationModule from '@/assets/images/tool-selection/wire/CommunicationModule.png'
import simCard from '@/assets/images/tool-selection/wire/SimCard.png'
import antenna from '@/assets/images/tool-selection/wire/Antenna.png'
import cableTieLabel from '@/assets/images/tool-selection/wire/CableTieLabel.png'

// ─── 柜体局部操作 — 背景图 / prompt 提示窗 ───
import cabinetGroupOverview from '@/assets/images/cabinet/CabinetGroupOverview.png'
// 铅封图：Sealed 计量/终端小室画布放置（1.54:1 横向）；CabinetLeadSeal 柜门门把铅封（1:1）
import sealed from '@/assets/images/cabinet/Sealed.png'
import cabinetLeadSeal from '@/assets/images/cabinet/CabinetLeadSeal.png'
// 步骤24 按钮图：清理现场 / 办理工作终结（含悬浮图）
import cleanButton from '@/assets/images/cabinet/CleanButton.png'
import cleanButtonHover from '@/assets/images/cabinet/CleanButtonHover.png'
import endButton from '@/assets/images/cabinet/EndButton.png'
import endButtonHover from '@/assets/images/cabinet/EndButtonHover.png'
import powerSocket from '@/assets/images/cabinet/PowerSocket.png'
import electrifyCompleteNotice from '@/assets/images/cabinet/ElectrifyCompleteNotice.png'
import meterRoomOperationSuccess from '@/assets/images/cabinet/MeterRoomOperationSuccess.png'
import readyForPowerOnNotice from '@/assets/images/cabinet/ReadyForPowerOnNotice.png'
import checkMeterReading from '@/assets/images/cabinet/CheckMeterReading.png'
import verifyMeterDataConsistency from '@/assets/images/cabinet/VerifyMeterDataConsistency.png'
import verifyTerminalWiring from '@/assets/images/cabinet/VerifyTerminalWiring.png'

// ─── 步骤12 柜体局部 — 线材垃圾素材 ───
import wireTrash1 from '@/assets/images/cabinet/WireTrash1.png'
import wireTrash2 from '@/assets/images/cabinet/WireTrash2.png'
import wireTrash3 from '@/assets/images/cabinet/WireTrash3.png'

// ─── 柜体操作 — 左侧工具栏（围栏/告示牌） ───
import barLeftFence from '@/assets/images/cabinet/toolbar/LeftFence.png'
import barRightFence from '@/assets/images/cabinet/toolbar/RightFence.png'
import barSignStopHighVoltage from '@/assets/images/cabinet/toolbar/SignStopHighVoltage.png'
import barSignPersonWorking from '@/assets/images/cabinet/toolbar/SignPersonWorking.png'
import barSafetyNotice from '@/assets/images/cabinet/toolbar/SafetyNotice.png'

// ─── 柜体操作 — 右侧工具栏（终端/工器具/线材） ───
import barVoltageTesterNormal from '@/assets/images/cabinet/toolbar/VoltageTesterNormal.png'
import barVoltageTesterWarning from '@/assets/images/cabinet/toolbar/VoltageTesterWarning.png'
import barThreePhaseThreeWireMeter from '@/assets/images/cabinet/toolbar/ThreePhaseThreeWireMeter.png'
import barThreePhaseThreeWireTerminal from '@/assets/images/cabinet/toolbar/ThreePhaseThreeWireTerminal.png'
import barCrossScrewdriver from '@/assets/images/cabinet/toolbar/CrossScrewdriver.png'
import barWireStripper from '@/assets/images/cabinet/toolbar/WireStripper.png'
import barSeal from '@/assets/images/cabinet/toolbar/Seal.png'
// 线材通用图（1:1 工具栏版，4.0mm² 由 2.5mm² 图放大模拟）
import barWireYellow from '@/assets/images/cabinet/toolbar/WireYellow.png'
import barWireGreen from '@/assets/images/cabinet/toolbar/WireGreen.png'
import barWireRed from '@/assets/images/cabinet/toolbar/WireRed.png'
import barWire2Core from '@/assets/images/cabinet/toolbar/Wire2Core.png'
import barWire4mm2YellowBlack from '@/assets/images/cabinet/toolbar/Wire4mm2YellowBlack.png'
import barWire4mm2RedBlack from '@/assets/images/cabinet/toolbar/Wire4mm2RedBlack.png'
import barCableTieLabel from '@/assets/images/cabinet/toolbar/CableTieLabel.png'
import barSignalCable6Core from '@/assets/images/cabinet/toolbar/SignalCable6Core.png'
import barSignalCable8Core from '@/assets/images/cabinet/toolbar/SignalCable8Core.png'
import barCommunicationModule from '@/assets/images/cabinet/toolbar/CommunicationModule.png'
import barSimCard from '@/assets/images/cabinet/toolbar/SimCard.png'
import barAntenna from '@/assets/images/cabinet/toolbar/Antenna.png'

// ─── 计量小室（leafer ui 画布） ───
import meteringRoomNoMeter from '@/assets/images/cabinet/room/metering-room/MeteringRoomNoMeter.png'
import meteringRoomWithMeter from '@/assets/images/cabinet/room/metering-room/MeteringRoomWithMeter.png'
import meteringRoomWired from '@/assets/images/cabinet/room/metering-room/MeteringRoomWired.png'
import meteringRoomWithCableTies from '@/assets/images/cabinet/room/metering-room/MeteringRoomWithCableTies.png'
import meteringRoomCovered from '@/assets/images/cabinet/room/metering-room/MeteringRoomCovered.png'
import junctionBox from '@/assets/images/cabinet/room/JunctionBox.png'
import junctionBoxSwitch from '@/assets/images/cabinet/room/JunctionBoxSwitch.png'
import strippedSignalCable6Core from '@/assets/images/cabinet/room/metering-room/StrippedSignalCable6Core.png'

// ─── 终端小室（leafer ui 画布） ───
import terminalRoomNoMeter from '@/assets/images/cabinet/room/terminal-room/TerminalRoomNoMeter.png'
import terminalRoomWithMeter from '@/assets/images/cabinet/room/terminal-room/TerminalRoomWithMeter.png'
import terminalRoomWired from '@/assets/images/cabinet/room/terminal-room/TerminalRoomWired.png'
import terminalRoomCovered from '@/assets/images/cabinet/room/terminal-room/TerminalRoomCovered.png'
import terminalRoomPendingCommModule from '@/assets/images/cabinet/room/terminal-room/TerminalRoomPendingCommModule.png'
import terminalRoomWithAntenna from '@/assets/images/cabinet/room/terminal-room/TerminalRoomWithAntenna.png'
import terminalElectrifyCompleteNotice from '@/assets/images/cabinet/room/terminal-room/TerminalElectrifyCompleteNotice.png'
import remoteControlBoard from '@/assets/images/cabinet/room/terminal-room/RemoteControlBoard.png'
import remoteControlSwitch from '@/assets/images/cabinet/room/terminal-room/RemoteControlSwitch.png'
import terminalSignalCable8Core from '@/assets/images/cabinet/room/terminal-room/SignalCable8Core.png'
import remoteControlCable2Core from '@/assets/images/cabinet/room/terminal-room/RemoteControlCable2Core.png'
import remoteSignalCable2Core from '@/assets/images/cabinet/room/terminal-room/RemoteSignalCable2Core.png'
import signToMeteringRoom from '@/assets/images/cabinet/room/terminal-room/SignToMeteringRoom.png'
import signToOutletCabinet from '@/assets/images/cabinet/room/terminal-room/SignToOutletCabinet.png'
import terminalAntenna from '@/assets/images/cabinet/Antenna.png'
import terminalRoomCompleteNotice from '@/assets/images/cabinet/TerminalRoomCompleteNotice.png'

export default {
  // 通用
  banner,
  mjuLogo,
  confirmButtonYellow,
  confirmButtonYellowHover,
  confirmButtonGreen,
  confirmButtonGreenHover,
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
  workTicketBackground,
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
  // 工器具选择
  // 2.终端
  threePhaseThreeWireTerminal,
  threePhaseFourWireTerminal,
  terminal230M,
  concentrator,
  collector,
  // 3.工器具
  flatheadScrewdriver,
  crossScrewdriver,
  adjustableWrench,
  needleNosePliers,
  slipJointPliers,
  wireStripper,
  voltageTester,
  seal,
  // 4.线材（3:5 选择页版；4.0mm² 复用 2.5mm² 图，与柜体工具栏惯例一致）
  wireGreen,
  wireRed,
  wireRedBlack,
  wireYellow,
  wireYellowBlack,
  wire25mmBlack,
  wire2Core,
  signalCable6Core,
  signalCable8Core,
  communicationModule,
  simCard,
  antenna,
  cableTieLabel,
  // 柜体操作 — 背景图 / prompt 提示窗
  cabinetGroupOverview,
  sealed,
  cabinetLeadSeal,
  cleanButton,
  cleanButtonHover,
  endButton,
  endButtonHover,
  powerSocket,
  electrifyCompleteNotice,
  meterRoomOperationSuccess,
  terminalRoomCompleteNotice,
  terminalElectrifyCompleteNotice,
  readyForPowerOnNotice,
  checkMeterReading,
  verifyMeterDataConsistency,
  verifyTerminalWiring,
  // 步骤12 终端小室 — 线材垃圾
  wireTrash1,
  wireTrash2,
  wireTrash3,
  // 柜体操作 — 左侧工具栏（围栏/告示牌）
  barLeftFence,
  barRightFence,
  barSignStopHighVoltage,
  barSignPersonWorking,
  barSafetyNotice,
  // 柜体操作 — 右侧工具栏（终端/工器具/线材）
  barThreePhaseThreeWireMeter,
  barThreePhaseThreeWireTerminal,
  barCrossScrewdriver,
  barWireStripper,
  barVoltageTesterNormal,
  barVoltageTesterWarning,
  barSeal,
  barWireYellow,
  barWireGreen,
  barWireRed,
  barWire2Core,
  barWire4mm2YellowBlack,
  barWire4mm2RedBlack,
  barCableTieLabel,
  barSignalCable6Core,
  barSignalCable8Core,
  barCommunicationModule,
  barSimCard,
  barAntenna,
  // 计量小室（leafer ui 画布）
  meteringRoomNoMeter,
  meteringRoomWithMeter,
  meteringRoomWired,
  meteringRoomWithCableTies,
  meteringRoomCovered,
  junctionBox,
  junctionBoxSwitch,
  strippedSignalCable6Core,
  // 终端小室（leafer ui 画布）
  terminalRoomNoMeter,
  terminalRoomWithMeter,
  terminalRoomWired,
  terminalRoomCovered,
  terminalRoomPendingCommModule,
  terminalRoomWithAntenna,
  remoteControlBoard,
  remoteControlSwitch,
  terminalSignalCable8Core,
  remoteControlCable2Core,
  remoteSignalCable2Core,
  signToMeteringRoom,
  signToOutletCabinet,
  terminalAntenna
}

/**
 * CSS 变量映射（供 main.js 注入）
 * CSS 中用 var(--img-xxx) 引用，更换格式只改上方 import
 */
export const cssVars = {
  '--img-confirm-btn': confirmButtonYellow,
  '--img-confirm-btn-hover': confirmButtonYellowHover,
  // 绿色确认键（计量小室画布专用，其他按钮保持黄色）
  '--img-confirm-btn-green': confirmButtonGreen,
  '--img-confirm-btn-green-hover': confirmButtonGreenHover,
  // 步骤24 清理现场 / 办理工作终结（背景图 + hover 图）
  '--img-clean-btn': cleanButton,
  '--img-clean-btn-hover': cleanButtonHover,
  '--img-end-btn': endButton,
  '--img-end-btn-hover': endButtonHover,
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
  '--img-hwt-bg': workTicketBackground
}
