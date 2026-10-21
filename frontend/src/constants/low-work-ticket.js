// 两张工作票的字母顺序不同，存档和校验统一使用内容标识。
const OPTION_TEXT = {
  voltage: '防止电压回路短路或接地；',
  current: '严禁电流互感器二次回路开路；',
  distance: '与带电设备保持足够安全距离。',
  education:
    '工作负责人对工作班成员应进行安全教育，作业前对工作班成员进行危险点告知，明确带电设备位置，交代工作地点及周围的带电部位及安全措施和技术措施，并履行确认手续。',
  sign: '检查是否悬挂指示牌。',
  task: '核对工作任务单与现场信息是否一致。',
  wire: '检查接入电源的电线有无破损，接入电源应固定牢靠。',
  monitor: '防止操作时相间或相对地短路，加强移动监护。',
  leakage: '移动电源盘，应带漏电保护器。',
  tools: '使用合格的工器具，螺丝刀除刀口以外的金属裸露部分应用绝缘胶布包裹。'
}

function createOptions(values) {
  return values.map((value, index) => ({
    value,
    label: OPTION_TEXT[value] || value,
    letter: String.fromCharCode(65 + index)
  }))
}

export const LOW_TICKETS = [
  {
    id: 'concentrator',
    label: '集中器',
    ticketNo: '1001',
    location: '福州台江江滨3#变压器低压计量表箱',
    task: '集中器安装与调试',
    taskLocationLines: ['福州台江江滨3#变压器', '低压计量表箱'],
    options: {
      team: createOptions(['营销班', '检测班', '装表班', '抄表班']),
      leader: createOptions(['王五', '张亮', '张三', '张五']),
      member2: createOptions(['张五', '张亮', '张三', '王五']),
      safetyMeasures: createOptions(['current', 'voltage', 'distance']),
      dangerInterval: createOptions(['education', 'sign', 'task', 'wire']),
      dangerShortCircuit: createOptions(['monitor', 'wire', 'leakage', 'tools'])
    }
  },
  {
    id: 'meter',
    label: '智能电表',
    ticketNo: '1002',
    location: '福州江滨小区5号楼一层居民表箱110012000011',
    task: '单相电能表安装',
    taskLocationLines: ['福州江滨小区5号楼一层', '居民表箱110012000011'],
    options: {
      team: createOptions(['营销班', '抄表班', '检测班', '装表班']),
      leader: createOptions(['张五', '张三', '张亮', '王五']),
      member2: createOptions(['张五', '王五', '张三', '张亮']),
      safetyMeasures: createOptions(['voltage', 'distance', 'current']),
      dangerInterval: createOptions(['education', 'sign', 'task', 'wire']),
      dangerShortCircuit: createOptions(['wire', 'monitor', 'tools', 'leakage'])
    }
  }
]

export const LOW_TICKET_ANSWERS = {
  team: '装表班',
  leader: '张亮',
  member2: '张三',
  safetyMeasures: ['voltage', 'current', 'distance'],
  dangerInterval: ['education', 'sign', 'task'],
  dangerShortCircuit: ['wire', 'monitor', 'tools', 'leakage']
}

/** 生成低压双工作票初始存档，时间快照仅在新建时生成。 */
export function createLowTicketData(playerName = '') {
  const start = new Date()
  const end = new Date(start)
  end.setHours(18, 0, 0, 0)
  if (end <= start) end.setDate(end.getDate() + 1)
  const sign = new Date(start)
  sign.setDate(sign.getDate() - 1)
  return {
    version: 1,
    activeTicket: 'concentrator',
    scrollPositions: { concentrator: 0, meter: 0 },
    tickets: Object.fromEntries(
      LOW_TICKETS.map(ticket => [
        ticket.id,
        {
          company: '',
          ticketNo: ticket.ticketNo,
          team: '',
          leader: '',
          member1: playerName,
          member2: '',
          safetyMeasures: [],
          dangerInterval: [],
          dangerShortCircuit: [],
          planStart: formatTicketTime(start),
          planEnd: formatTicketTime(end),
          signDate: formatTicketTime(sign)
        }
      ])
    )
  }
}

/** 格式化工作票上的中文日期。 */
export function formatTicketTime(date) {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}年${pad(date.getMonth() + 1)}月${pad(date.getDate())}日 ${pad(date.getHours())}时${pad(date.getMinutes())}分`
}

/** 校验两张票，按正文顺序返回首个及全部错误。 */
export function validateLowTickets(data) {
  const errors = []
  const labels = {
    company: '单位',
    member1: '第一位工作班人员',
    team: '工作班组',
    leader: '工作负责人',
    member2: '第二位工作班人员',
    safetyMeasures: '注意事项',
    dangerInterval: '走错工作间隔防范措施',
    dangerShortCircuit: '短路或接地防范措施'
  }
  for (const ticket of LOW_TICKETS) {
    const form = data.tickets[ticket.id]
    for (const field of [
      'company',
      'team',
      'leader',
      'member1',
      'member2',
      'safetyMeasures',
      'dangerInterval',
      'dangerShortCircuit'
    ]) {
      const value = form[field]
      const expected = field === 'company' ? '福州' : LOW_TICKET_ANSWERS[field]
      const valid =
        field === 'member1'
          ? typeof value === 'string' && !!value.trim()
          : Array.isArray(expected)
            ? Array.isArray(value) &&
              value.length === expected.length &&
              expected.every(v => value.includes(v))
            : typeof value === 'string' && value.trim() === expected
      if (!valid)
        errors.push({
          ticket: ticket.id,
          field,
          message: `${ticket.label}：请核对${labels[field]}`
        })
    }
  }
  return errors
}
