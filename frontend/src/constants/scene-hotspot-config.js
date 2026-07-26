/**
 * 配电室场景热区配置（工器具选择之后）
 *
 * 坐标均为相对场景画面（SceneFrame）的百分比。
 * URL 加 ?debugHotspot=1 可半透明显示热区，便于标定。
 */

import overviewBg from '@/assets/images/scene-distribution-room.png'
import cabinetLocalBg from '@/assets/images/scene-cabinet-local.png'

export const HOTSPOT_DEBUG_DEFAULT = false

export const cabinetVisitedKey = 'hvCabinetVisited'
export const selectedToolsKey = 'hvSelectedTools'

/** A1 配电室总览（964×497） */
export const sceneOverview = {
  id: 'overview',
  title: '配电室总览',
  background: overviewBg,
  aspectRatio: '964 / 497',
  tip: '请点击「进线柜」或「计量柜」进入局部操作',
  hotspots: [
    // —— 后墙左侧一排 ——
    { id: 'dc', label: '直流柜', top: '38%', left: '4%', width: '7%', height: '32%', correct: false },
    { id: 'signal', label: '信号柜', top: '28%', left: '11%', width: '7%', height: '42%', correct: false },
    {
      id: 'incoming-main',
      label: '进线柜',
      top: '26%',
      left: '18%',
      width: '10%',
      height: '44%',
      correct: true,
      targetRoute: '/HIncomingCabinet',
      visitKey: 'incoming'
    },
    {
      id: 'metering-main',
      label: '计量柜',
      top: '26%',
      left: '28%',
      width: '10%',
      height: '44%',
      correct: true,
      targetRoute: '/HMeteringCabinet',
      visitKey: 'metering'
    },
    { id: 'outgoing-1', label: '出线柜', top: '26%', left: '38%', width: '9%', height: '44%', correct: false },
    { id: 'pt', label: 'PT柜', top: '26%', left: '47%', width: '9%', height: '44%', correct: false },

    // —— 右侧墙 ——
    { id: 'capacitor', label: '电容补偿柜', top: '24%', left: '56%', width: '8%', height: '42%', correct: false },
    { id: 'outgoing-2', label: '出线柜', top: '24%', left: '64%', width: '8%', height: '42%', correct: false },
    { id: 'outgoing-3', label: '出线柜', top: '24%', left: '72%', width: '7%', height: '42%', correct: false },
    {
      id: 'incoming-right',
      label: '进线柜',
      top: '24%',
      left: '79%',
      width: '8%',
      height: '42%',
      correct: true,
      targetRoute: '/HIncomingCabinet',
      visitKey: 'incoming'
    },
    { id: 'transformer', label: '变压器', top: '28%', left: '87%', width: '10%', height: '40%', correct: false }
  ]
}

/**
 * 柜内局部界面（964×591）共用背景。
 * 画面左右自带侧栏区域，由 Vue 面板覆盖并展示真实数据。
 */
export const cabinetLocalScene = {
  background: cabinetLocalBg,
  aspectRatio: '964 / 591',
  /** 覆盖原图左右侧栏的区域（相对画面） */
  leftPanel: { top: '8%', left: '1.2%', width: '8.5%', height: '84%' },
  rightPanel: { top: '8%', left: '90.3%', width: '8.5%', height: '84%' },
  hotspots: [
    {
      id: 'local-incoming',
      label: '进线柜',
      top: '14%',
      left: '13%',
      width: '16%',
      height: '65%',
      correct: true,
      visitKey: 'incoming'
    },
    {
      id: 'local-metering',
      label: '计量柜',
      top: '14%',
      left: '29%',
      width: '17%',
      height: '65%',
      correct: true,
      visitKey: 'metering'
    },
    {
      id: 'local-outgoing',
      label: '出线柜',
      top: '14%',
      left: '56%',
      width: '27%',
      height: '65%',
      correct: false
    }
  ]
}

/** 本次操作新增物品（正式图标未到位前用占位符替代） */
export const localStepNewItems = [
  { id: 'fence', name: '安全围栏', icon: '🚧' },
  { id: 'hv-danger', name: '高压危险牌', icon: '⚠️' },
  { id: 'work-here', name: '在此工作牌', icon: '🪧' },
  { id: 'no-close', name: '禁止合闸牌', icon: '🚫' },
  { id: 'ground-wire', name: '接地线', icon: '🔌' },
  { id: 'insulated-glove', name: '绝缘手套', icon: '🧤' },
  { id: 'voltage-tester', name: '验电器', icon: '⚡' },
  { id: 'earthing-rod', name: '接地棒', icon: '🔧' }
]

export const incomingCabinet = {
  id: 'incoming',
  title: '进线柜局部操作',
  tip: '左侧为本步新物品，右侧为已选工器具',
  ...cabinetLocalScene
}

export const meteringCabinet = {
  id: 'metering',
  title: '计量柜局部操作',
  tip: '左侧为本步新物品，右侧为已选工器具',
  ...cabinetLocalScene
}
