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
 *   - description: 简要描述 (可选，用于卡片展示)
 */

export const categories = [
  {
    key: 'ppe',
    title: '个人防护',
    icon: 'UserFilled',
    requiredCount: 5,

    correctIds: [1, 5, 7, 9, 10],

    tools: [
      { id: 1,  name: '工作服',             slot: 'body', icon: '👔', description: '全棉长袖工作服' },
      { id: 2,  name: 'T恤和短裤',          slot: 'body', icon: '👕', description: '不符合作业着装规范' },
      { id: 3,  name: '工作服上装和牛仔裤',   slot: 'body', icon: '👖', description: '非全套工作服' },

      { id: 4,  name: '红色安全帽',          slot: 'head', icon: '⛑️', description: '管理人员用安全帽' },
      { id: 5,  name: '蓝色安全帽',          slot: 'head', icon: '🪖', description: '作业人员用安全帽' },
      { id: 6,  name: '摩托车帽',            slot: 'head', icon: '🪖', description: '不具备安全防护功能' },

      { id: 7,  name: '护目镜',              slot: 'eye',  icon: '🥽', description: '防飞溅物伤害眼睛' },

      { id: 8,  name: '橡胶手套',            slot: 'hand', icon: '🧤', description: '绝缘防护，防触电' },
      { id: 9,  name: '纱手套',              slot: 'hand', icon: '🧤', description: '具备绝缘性能' },

      { id: 10, name: '电力绝缘鞋',          slot: 'foot', icon: '🥾', description: '绝缘防砸，作业必备' },
      { id: 11, name: '凉鞋',                slot: 'foot', icon: '🩴', description: '不符合安全规范' },
      { id: 12, name: '皮鞋',                slot: 'foot', icon: '👞', description: '不具备绝缘性能' }
    ]
  },

  {
    key: 'terminal',
    title: '终端',
    icon: 'Monitor',
    requiredCount: null,

    correctIds: [21],

    tools: [
      { id: 21, name: '三相三线专变终端',    slot: 'device', icon: '📟', description: '适用于三相三线专变用户' },
      { id: 22, name: '三相四线专变终端',    slot: 'device', icon: '📟', description: '适用于三相四线专变用户' },
      { id: 23, name: '230M终端',            slot: 'device', icon: '📡', description: '230MHz无线通信终端' },
      { id: 24, name: '集中器',              slot: 'device', icon: '🖥️', description: '低压集抄用集中器' },
      { id: 25, name: '采集器',              slot: 'device', icon: '📷', description: '低压集抄用采集器' }
    ]
  },

  {
    key: 'tool',
    title: '工器具',
    icon: 'SetUp',
    requiredCount: null,

    correctIds: [32, 36, 37, 38],

    tools: [
      { id: 31, name: '一字螺丝刀',  slot: 'tool', icon: '🪛', description: '平口螺丝刀' },
      { id: 32, name: '十字螺丝刀',  slot: 'tool', icon: '🪛', description: '十字口螺丝刀，接线必备' },
      { id: 33, name: '活动扳手',    slot: 'tool', icon: '🔧', description: '可调开口扳手' },
      { id: 34, name: '尖嘴钳',      slot: 'tool', icon: '🔧', description: '尖嘴夹持工具' },
      { id: 35, name: '老虎钳',      slot: 'tool', icon: '🔧', description: '通用夹持剪切工具' },
      { id: 36, name: '剥线钳',      slot: 'tool', icon: '🔧', description: '剥除导线绝缘层' },
      { id: 37, name: '验电笔',      slot: 'tool', icon: '🖊️', description: '检验设备是否带电' },
      { id: 38, name: '铅封',        slot: 'tool', icon: '🔒', description: '防止私自开启设备' }
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
      { id: 51, name: '2.5MM²黄色导线',    slot: 'wire', icon: '🟡', description: '2.5平方毫米黄色' },
      { id: 52, name: '2.5MM²黄黑色导线',  slot: 'wire', icon: '🟡⚫', description: '2.5平方毫米黄黑双色' },
      { id: 53, name: '2.5MM²绿色导线',    slot: 'wire', icon: '🟢', description: '2.5平方毫米绿色' },
      { id: 54, name: '2.5MM²红色导线',    slot: 'wire', icon: '🔴', description: '2.5平方毫米红色' },
      { id: 55, name: '2.5MM²红黑色导线',  slot: 'wire', icon: '🔴⚫', description: '2.5平方毫米红黑双色' },
      { id: 56, name: '2.5MM²黑色导线',    slot: 'wire', icon: '⚫', description: '2.5平方毫米黑色' },

      { id: 57, name: '4.0MM²黄色导线',    slot: 'wire', icon: '🟡', description: '4.0平方毫米黄色' },
      { id: 58, name: '4.0MM²黄黑色导线',  slot: 'wire', icon: '🟡⚫', description: '4.0平方毫米黄黑双色' },
      { id: 59, name: '4.0MM²绿色导线',    slot: 'wire', icon: '🟢', description: '4.0平方毫米绿色' },
      { id: 60, name: '4.0MM²红色导线',    slot: 'wire', icon: '🔴', description: '4.0平方毫米红色' },
      { id: 61, name: '4.0MM²红黑色导线',  slot: 'wire', icon: '🔴⚫', description: '4.0平方毫米红黑双色' },

      { id: 62, name: '扎带标示牌',         slot: 'wire', icon: '🏷️', description: '线缆扎带和标示牌' },
      { id: 63, name: '2芯遥控线',          slot: 'wire', icon: '🔌', description: '2芯遥控信号线' },
      { id: 64, name: '2芯遥信线',          slot: 'wire', icon: '🔌', description: '2芯遥信信号线' },
      { id: 65, name: '6芯信号线',          slot: 'wire', icon: '🔌', description: '6芯信号连接线' },
      { id: 66, name: '8芯信号线',          slot: 'wire', icon: '🔌', description: '8芯信号连接线' },

      { id: 67, name: '通信模块',            slot: 'wire', icon: '📶', description: '无线通信模块' },
      { id: 68, name: 'SIM卡',              slot: 'wire', icon: '💳', description: '终端通信SIM卡' },
      { id: 69, name: '天线',               slot: 'wire', icon: '📡', description: '终端通信天线' }
    ]
  }
]

/**
 * 装备槽位显示配置
 */
export const slotLabels = {
  body:   '身体',
  head:   '头部',
  eye:    '眼部',
  hand:   '手部',
  foot:   '足部',
  device: '终端',
  tool:   '工器具',
  wire:   '线材'
}
