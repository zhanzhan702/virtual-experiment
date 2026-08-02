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

// ─── 柜体局部操作 — 背景图 ───
import cabinetGroupOverview from '@/assets/images/cabinet/CabinetGroupOverview.png'
import powerSocket from '@/assets/images/cabinet/PowerSocket.png'
import electrifyCompleteNotice from '@/assets/images/cabinet/ElectrifyCompleteNotice.png'

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
import barWire25mm2Yellow from '@/assets/images/cabinet/toolbar/Wire25mm2Yellow.png'
import barWire4mm2Yellow from '@/assets/images/cabinet/toolbar/Wire4mm2Yellow.png'
import barWire4mm2YellowBlack from '@/assets/images/cabinet/toolbar/Wire4mm2YellowBlack.png'
import barWire25mm2Green from '@/assets/images/cabinet/toolbar/Wire25mm2Green.png'
import barWire25mm2Red from '@/assets/images/cabinet/toolbar/Wire25mm2Red.png'
import barWire4mm2Red from '@/assets/images/cabinet/toolbar/Wire4mm2Red.png'
import barWire4mm2RedBlack from '@/assets/images/cabinet/toolbar/Wire4mm2RedBlack.png'
import barCableTieLabel from '@/assets/images/cabinet/toolbar/CableTieLabel.png'
import barRemoteControlCable2Core from '@/assets/images/cabinet/toolbar/RemoteControlCable2Core.png'
import barRemoteSignalCable2Core from '@/assets/images/cabinet/toolbar/RemoteSignalCable2Core.png'
import barSignalCable6Core from '@/assets/images/cabinet/toolbar/SignalCable6Core.png'
import barSignalCable8Core from '@/assets/images/cabinet/toolbar/SignalCable8Core.png'
import barCommunicationModule from '@/assets/images/cabinet/toolbar/CommunicationModule.png'
import barSimCard from '@/assets/images/cabinet/toolbar/SimCard.png'
import barAntenna from '@/assets/images/cabinet/toolbar/Antenna.png'

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
  // 柜体操作 — 背景图
  cabinetGroupOverview,
  powerSocket,
  electrifyCompleteNotice,
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
  barWire25mm2Yellow,
  barWire4mm2Yellow,
  barWire4mm2YellowBlack,
  barWire25mm2Green,
  barWire25mm2Red,
  barWire4mm2Red,
  barWire4mm2RedBlack,
  barCableTieLabel,
  barRemoteControlCable2Core,
  barRemoteSignalCable2Core,
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
