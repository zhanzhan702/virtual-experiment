package com.example.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.experiment.dto.admin.UserExperimentVO;
import com.example.experiment.entity.UserExperiments;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserExperimentsMapper extends BaseMapper<UserExperiments> {

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
