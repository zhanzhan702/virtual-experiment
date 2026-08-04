/**
 * 工器具选择配置
 *
 * 字段说明：
 * - key: 分类标识
 * - title: 分类中文标题
 * - icon: 分类图标 (Element Plus Icon 名称)
 * - requiredCount: 本分类最少需要选择的数量 (null 表示必须全部选对)
 * - correctIds: 正确答案的 ID 列表
 * - tools: 该分类下的所有工器具
 *   - id: 唯一标识
 *   - name: 工器具名称
 *   - slot: 装备槽位 (用于右侧人物装备面板展示)
 *   - icon: 工器具图标 (emoji)
 */

export const categories = [
  {
    key: 'ppe',
    title: '个人防护',
    icon: 'UserFilled',
    requiredCount: 5,

    correctIds: [1, 5, 7, 9, 10],

    tools: [
      {
        id: 1,
        name: '工作服',
        slot: 'body',
        icon: '👔',
        image: 'workClothes'
      },
      {
        id: 2,
        name: 'T恤和短裤',
        slot: 'body',
        icon: '👕',
        image: 'tShirtAndShorts'
      },
      {
        id: 3,
        name: '工作服上装和牛仔裤',
        slot: 'body',
        icon: '👖',
        image: 'workClothesTopAndJeans'
      },

      {
        id: 4,
        name: '红色安全帽',
        slot: 'head',
        icon: '⛑️',
        image: 'redSafetyHelmet'
      },
      {
        id: 5,
        name: '蓝色安全帽',
        slot: 'head',
        icon: '🪖',
        image: 'blueSafetyHelmet'
      },
      {
        id: 6,
        name: '摩托车帽',
        slot: 'head',
        icon: '🪖',
        image: 'motorcycleHelmet'
      },

      {
        id: 7,
        name: '护目镜',
        slot: 'eye',
        icon: '🥽',
        image: 'safetyGoggles'
      },

      {
        id: 8,
        name: '橡胶手套',
        slot: 'hand',
        icon: '🧤',
        image: 'rubberGloves'
      },
      {
        id: 9,
        name: '纱手套',
        slot: 'hand',
        icon: '🧤',
        image: 'cottonGloves'
      },

      {
        id: 10,
        name: '电力绝缘鞋',
        slot: 'foot',
        icon: '🥾',
        image: 'electricalInsulatingShoes'
      },
      {
        id: 11,
        name: '凉鞋',
        slot: 'foot',
        icon: '🩴',
        image: 'sandal'
      },
      {
        id: 12,
        name: '皮鞋',
        slot: 'foot',
        icon: '👞',
        image: 'leatherShoes'
      }
    ]
  },

  {
    key: 'terminal',
    title: '终端',
    icon: 'Monitor',
    requiredCount: null,

    correctIds: [21],

    tools: [
      {
        id: 21,
        name: '三相三线专变终端',
        slot: 'device',
        icon: '📟',
        image: 'threePhaseThreeWireTerminal'
      },
      {
        id: 22,
        name: '三相四线专变终端',
        slot: 'device',
        icon: '📟',
        image: 'threePhaseFourWireTerminal'
      },
      {
        id: 23,
        name: '230M终端',
        slot: 'device',
        icon: '📡',
        image: 'terminal230M'
      },
      {
        id: 24,
        name: '集中器',
        slot: 'device',
        icon: '🖥️',
        image: 'concentrator'
      },
      {
        id: 25,
        name: '采集器',
        slot: 'device',
        icon: '📷',
        image: 'collector'
      }
    ]
  },

  {
    key: 'tool',
    title: '工器具',
    icon: 'SetUp',
    requiredCount: null,

    correctIds: [32, 36, 37, 38],

    tools: [
      {
        id: 31,
        name: '一字螺丝刀',
        slot: 'tool',
        icon: '🪛',
        image: 'flatheadScrewdriver'
      },
      {
        id: 32,
        name: '十字螺丝刀',
        slot: 'tool',
        icon: '🪛',
        image: 'crossScrewdriver'
      },
      {
        id: 33,
        name: '活动扳手',
        slot: 'tool',
        icon: '🔧',
        image: 'adjustableWrench'
      },
      {
        id: 34,
        name: '尖嘴钳',
        slot: 'tool',
        icon: '🔧',
        image: 'needleNosePliers'
      },
      {
        id: 35,
        name: '老虎钳',
        slot: 'tool',
        icon: '🔧',
        image: 'slipJointPliers'
      },
      {
        id: 36,
        name: '剥线钳',
        slot: 'tool',
        icon: '🔧',
        image: 'wireStripper'
      },
      {
        id: 37,
        name: '验电笔',
        slot: 'tool',
        icon: '🖊️',
        image: 'voltageTester'
      },
      {
        id: 38,
        name: '铅封',
        slot: 'tool',
        icon: '🔒',
        image: 'seal'
      }
    ]
  },

  {
    key: 'wire',
    title: '线材',
    icon: 'Connection',
    requiredCount: null,
    multi: true,

    correctIds: [51, 53, 54, 57, 58, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69],

    tools: [
      { id: 51, name: '2.5MM²黄色导线', slot: 'wire', icon: '🟡' },
      {
        id: 52,
        name: '2.5MM²黄黑色导线',
        slot: 'wire',
        icon: '🟡⚫'
      },
      { id: 53, name: '2.5MM²绿色导线', slot: 'wire', icon: '🟢' },
      { id: 54, name: '2.5MM²红色导线', slot: 'wire', icon: '🔴' },
      {
        id: 55,
        name: '2.5MM²红黑色导线',
        slot: 'wire',
        icon: '🔴⚫'
      },
      { id: 56, name: '2.5MM²黑色导线', slot: 'wire', icon: '⚫' },

      { id: 57, name: '4.0MM²黄色导线', slot: 'wire', icon: '🟡' },
      {
        id: 58,
        name: '4.0MM²黄黑色导线',
        slot: 'wire',
        icon: '🟡⚫'
      },
      { id: 59, name: '4.0MM²绿色导线', slot: 'wire', icon: '🟢' },
      { id: 60, name: '4.0MM²红色导线', slot: 'wire', icon: '🔴' },
      {
        id: 61,
        name: '4.0MM²红黑色导线',
        slot: 'wire',
        icon: '🔴⚫'
      },

      { id: 62, name: '扎带标示牌', slot: 'wire', icon: '🏷️' },
      { id: 63, name: '2芯遥控线', slot: 'wire', icon: '🔌' },
      { id: 64, name: '2芯遥信线', slot: 'wire', icon: '🔌' },
      { id: 65, name: '6芯信号线', slot: 'wire', icon: '🔌' },
      { id: 66, name: '8芯信号线', slot: 'wire', icon: '🔌' },

      { id: 67, name: '通信模块', slot: 'wire', icon: '📶' },
      { id: 68, name: 'SIM卡', slot: 'wire', icon: '💳' },
      { id: 69, name: '天线', slot: 'wire', icon: '📡' }
    ]
  }
]

/**
 * 装备槽位显示配置
 */
export const slotLabels = {
  body: '身体',
  head: '头部',
  eye: '眼部',
  hand: '手部',
  foot: '足部',
  device: '终端',
  tool: '工器具',
  wire: '线材'
}
