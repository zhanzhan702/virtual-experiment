package com.example.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.experiment.dto.admin.UserExperimentVO;
import com.example.experiment.dto.admin.UserScoreRowVO;
import com.example.experiment.entity.UserExperiments;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserExperimentsMapper extends BaseMapper<UserExperiments> {

  /**
   * 批量取多个学生的已完成实验分数行，用于成绩表与班级汇总。
   *
   * <p>只取 {@code status = 1} 的记录 —— 进行中的实验 {@code score} 为 NULL，不参与成绩统计。
   * 分母用子查询实时求和，不硬编码满分，模板步骤分值调整后自动跟随。
   *
   * <p>调用方保证 userIds 非空（空列表会导致 IN () 语法错误）。
   */
  @Select(
      """
      <script>
      SELECT e.user_id       AS userId,
             e.id            AS experimentId,
             t.category      AS category,
             e.score         AS rawScore,
             e.manual_score  AS manualScore,
             sums.total      AS stepTotal
      FROM user_experiments e
      JOIN experiment_templates t ON t.id = e.template_id
      LEFT JOIN (
             SELECT template_id, SUM(score) AS total
             FROM experiment_steps
             GROUP BY template_id
           ) sums ON sums.template_id = e.template_id
      WHERE e.status = 1
        AND e.user_id IN
        <foreach collection="userIds" item="uid" open="(" separator="," close=")">
          #{uid}
        </foreach>
      </script>
      """)
  List<UserScoreRowVO> selectScoreRows(@Param("userIds") List<String> userIds);

  /**
   * 查询某学生的全部实验记录（含进行中的），供成绩详情弹窗的完成历史使用。
   *
   * <p>与 {@link #selectScoreRows} 的区别：不按 status 过滤，因为历史要展示「进行中」的记录。
   */
  @Select(
      """
      SELECT e.user_id       AS userId,
             e.id            AS experimentId,
             t.category      AS category,
             t.name          AS templateName,
             e.status        AS status,
             e.score         AS rawScore,
             e.manual_score  AS manualScore,
             sums.total      AS stepTotal,
             e.start_time    AS startTime,
             e.end_time      AS endTime,
             e.total_duration AS totalDuration,
             e.scored_at     AS scoredAt
      FROM user_experiments e
      JOIN experiment_templates t ON t.id = e.template_id
      LEFT JOIN (
             SELECT template_id, SUM(score) AS total
             FROM experiment_steps
             GROUP BY template_id
           ) sums ON sums.template_id = e.template_id
      WHERE e.user_id = #{userId}
      ORDER BY e.start_time DESC, e.id
      """)
  List<UserScoreRowVO> selectStudentRows(@Param("userId") String userId);

  /**
   * 查询某用户的全部实验记录（含模板名与类型），按开始时间倒序。
   *
   * <p>start_time 允许为 NULL（实验创建后未开始），排序时 NULL 会排在最前， 因此补一个 id 作为稳定次序。
   */
  @Select(
      """
      SELECT e.id             AS experimentId,
             t.name           AS templateName,
             t.category       AS category,
             e.status         AS status,
             e.score          AS score,
             e.start_time     AS startTime,
             e.end_time       AS endTime,
             e.total_duration AS totalDuration
      FROM user_experiments e
      LEFT JOIN experiment_templates t ON t.id = e.template_id
      WHERE e.user_id = #{userId}
      ORDER BY e.start_time DESC, e.id
      """)
  List<UserExperimentVO> selectUserExperiments(@Param("userId") String userId);
}
