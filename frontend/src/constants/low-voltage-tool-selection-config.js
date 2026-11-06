/**
 * 低压工器具选择配置。
 *
 * 四个分类里只有「线材」与高压不同：低压用 4mm²绿黑线、0.5mm²遥控线、0.5mm²遥信线、
 * RS485 线替换了高压的 6 芯 / 8 芯信号线。个人防护、终端、工器具的目录与正确项
 * 与高压完全一致（correctIds 也相同）。
 *
 * requiredCount 一律留空：组件用 `requiredCount ?? correctIds.length` 推导应选数量，
 * 这样只需维护 correctIds 一处，不会出现「应选 5 个但只标了 4 个正确项」的矛盾。
 *
 * 正确项来源：仿真操作手册「个人防护、终端、工器具、线材选择界面」一节的标准答案。
 */

const ppe = [
  { id: 1, name: '工作服', slot: 'body', icon: '👔', image: 'workClothes' },
  { id: 2, name: 'T恤和短裤', slot: 'body', icon: '👕', image: 'tShirtAndShorts' },
  { id: 3, name: '工作服上装和牛仔裤', slot: 'body', icon: '👖', image: 'workClothesTopAndJeans' },
  { id: 4, name: '红色安全帽', slot: 'head', icon: '⛑️', image: 'redSafetyHelmet' },
  { id: 5, name: '蓝色安全帽', slot: 'head', icon: '🪖', image: 'blueSafetyHelmet' },
  { id: 6, name: '摩托车帽', slot: 'head', icon: '🪖', image: 'motorcycleHelmet' },
  { id: 7, name: '护目镜', slot: 'eye', icon: '🥽', image: 'safetyGoggles' },
  { id: 8, name: '橡胶手套', slot: 'hand', icon: '🧤', image: 'rubberGloves' },
  { id: 9, name: '纱手套', slot: 'hand', icon: '🧤', image: 'cottonGloves' },
  { id: 10, name: '电力绝缘鞋', slot: 'foot', icon: '🥾', image: 'electricalInsulatingShoes' },
  { id: 11, name: '凉鞋', slot: 'foot', icon: '🩴', image: 'sandal' },
  { id: 12, name: '皮鞋', slot: 'foot', icon: '👞', image: 'leatherShoes' }
]

const terminal = [
  { id: 21, name: '三相三线专变终端', slot: 'device', icon: '📟', image: 'threePhaseThreeWireTerminal' },
  { id: 22, name: '三相四线专变终端', slot: 'device', icon: '📟', image: 'threePhaseFourWireTerminal' },
  { id: 23, name: '230M终端', slot: 'device', icon: '📡', image: 'terminal230M' },
  { id: 24, name: '集中器', slot: 'device', icon: '🖥️', image: 'concentrator' },
  { id: 25, name: '采集器', slot: 'device', icon: '📷', image: 'collector' }
]

const tool = [
  { id: 31, name: '一字螺丝刀', slot: 'tool', icon: '🪛', image: 'flatheadScrewdriver' },
  { id: 32, name: '十字螺丝刀', slot: 'tool', icon: '🪛', image: 'crossScrewdriver' },
  { id: 33, name: '活动扳手', slot: 'tool', icon: '🔧', image: 'adjustableWrench' },
  { id: 34, name: '尖嘴钳', slot: 'tool', icon: '🔧', image: 'needleNosePliers' },
  { id: 35, name: '老虎钳', slot: 'tool', icon: '🔧', image: 'slipJointPliers' },
  { id: 36, name: '剥线钳', slot: 'tool', icon: '🔧', image: 'wireStripper' },
  { id: 37, name: '验电笔', slot: 'tool', icon: '🖊️', image: 'voltageTester' },
  { id: 38, name: '铅封', slot: 'tool', icon: '🔒', image: 'seal' }
]

// 顺序按原网页截图排列；4.0mm² 无独立素材，复用 2.5mm² 图（与高压惯例一致）
const wire = [
  { id: 51, name: '2.5MM²黄色导线', slot: 'wire', icon: '🟡', image: 'wireYellow' },
  { id: 52, name: '2.5MM²黄黑色导线', slot: 'wire', icon: '🟡⚫', image: 'wireYellowBlack' },
  { id: 53, name: '2.5MM²绿色导线', slot: 'wire', icon: '🟢', image: 'wireGreen' },
  { id: 54, name: '2.5MM²红色导线', slot: 'wire', icon: '🔴', image: 'wireRed' },
  { id: 55, name: '2.5MM²红黑色导线', slot: 'wire', icon: '🔴⚫', image: 'wireRedBlack' },
  { id: 56, name: '2.5MM²黑色导线', slot: 'wire', icon: '⚫', image: 'wire25mmBlack' },

  { id: 57, name: '4.0MM²黄色导线', slot: 'wire', icon: '🟡', image: 'wireYellow' },
  { id: 58, name: '4.0MM²黄黑色导线', slot: 'wire', icon: '🟡⚫', image: 'wireYellowBlack' },
  { id: 59, name: '4.0MM²绿色导线', slot: 'wire', icon: '🟢', image: 'wireGreen' },
  { id: 60, name: '4.0MM²红色导线', slot: 'wire', icon: '🔴', image: 'wireRed' },
  { id: 61, name: '4.0MM²红黑色导线', slot: 'wire', icon: '🔴⚫', image: 'wireRedBlack' },
  { id: 62, name: '扎带', slot: 'wire', icon: '🏷️', image: 'cableTieLabel' },

  { id: 63, name: '4.0MM²绿黑色导线', slot: 'wire', icon: '🟢⚫', image: 'wireGreenBlack' },
  { id: 64, name: '0.5MM²遥控线', slot: 'wire', icon: '🔌', image: 'wire2Core' },
  { id: 65, name: '0.5MM²遥信线', slot: 'wire', icon: '🔌', image: 'wire2Core' },
  { id: 66, name: 'RS485线', slot: 'wire', icon: '🔌', image: 'wireRS485' },
  { id: 67, name: '通信模块', slot: 'wire', icon: '📶', image: 'communicationModule' },
  { id: 68, name: 'SIM卡', slot: 'wire', icon: '💳', image: 'simCard' },
  { id: 69, name: '天线', slot: 'wire', icon: '📡', image: 'antenna' }
]

export const categories = [
  {
    key: 'ppe',
    title: '个人防护',
    icon: 'UserFilled',
    requiredCount: null,
    correctIds: [1, 5, 7, 9, 10],
    tools: ppe
  },
  {
    key: 'terminal',
    title: '终端',
    icon: 'Monitor',
    requiredCount: null,
    correctIds: [24],
    tools: terminal
  },
  {
    key: 'tool',
    title: '工器具',
    icon: 'SetUp',
    requiredCount: null,
    correctIds: [32, 36, 37, 38],
    tools: tool
  },
  {
    key: 'wire',
    title: '线材',
    icon: 'Connection',
    requiredCount: null,
    multi: true,
    // 正确 13 项：黄/绿/红/黑各规格 10 根 + 通信模块 + SIM卡 + 天线
    // 错误 6 项：2.5mm²黄黑、2.5mm²红黑、扎带、遥控线、遥信线、RS485线
    correctIds: [51, 53, 54, 56, 57, 58, 59, 60, 61, 63, 67, 68, 69],
    tools: wire
  }
]
