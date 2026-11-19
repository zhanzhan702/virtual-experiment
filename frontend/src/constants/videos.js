/**
 * 视频资源统一导出
 * 新增视频只需修改此文件，组件无需改动
 *
 * 使用方式：
 *   import Videos from '@/constants/videos'
 *   // 模板: :src="Videos.testVideo"
 *   // JS:   Videos.testVideo
 */

// ─── 高压前三个教学视频（按实验流程顺序） ───
// 1：工作票提交后；2：工器具选择完成后；3：告示牌悬挂完成
import highTicketVideo from '@/assets/videos/HeighVoltage/1.mp4'
import highToolSelectionVideo from '@/assets/videos/HeighVoltage/2.mp4'
import highFenceVideo from '@/assets/videos/HeighVoltage/3.mp4'

// ─── 通用（其余教学视频仍用测试视频占位，待正式视频替换） ───
import testVideo from '@/assets/videos/test.mp4'

export default {
  highTicketVideo,
  highToolSelectionVideo,
  highFenceVideo,
  testVideo
}
