package com.example.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.experiment.entity.Organization;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {

  /**
   * 按 sort 排序列出全部组织节点。
   *
   * <p>组织架构总共只有几十个节点，一次性取回在 Java 里建树，比递归查库更简单也更快。
   */
  @Select(
      "SELECT id, name, type, parent_id AS parentId, path, sort FROM organization ORDER BY sort, name")
  List<Organization> selectAllOrdered();

  /** 按 path 前缀查询某节点及其所有子孙节点（含自身） */
  @Select(
      """
      SELECT id, name, type, parent_id AS parentId, path, sort
      FROM organization
      WHERE path LIKE CONCAT(#{pathPrefix}, '%')
      ORDER BY sort, name
      """)
  List<Organization> selectSubtree(@Param("pathPrefix") String pathPrefix);
}
