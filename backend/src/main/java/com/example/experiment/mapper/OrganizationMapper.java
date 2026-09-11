package com.example.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.experiment.dto.admin.OrgUserCountVO;
import com.example.experiment.entity.Organization;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

  /** 某节点的直接子节点数 */
  @Select("SELECT COUNT(*) FROM organization WHERE parent_id = #{parentId}")
  int countChildren(@Param("parentId") String parentId);

  /** 挂靠到某节点的用户数 */
  @Select("SELECT COUNT(*) FROM users WHERE org_id = #{orgId}")
  int countUsers(@Param("orgId") String orgId);

  /**
   * 批量统计各节点挂靠的用户数，用于组织树展示。
   *
   * <p>一次查回全部，避免为每个节点单独查库。
   */
  @Select(
      """
      SELECT HEX(u.org_id) AS orgId, COUNT(*) AS userCount
      FROM users u
      WHERE u.org_id IS NOT NULL
      GROUP BY u.org_id
      """)
  List<OrgUserCountVO> countUsersGroupedByOrg();

  /** 同级节点的最大 sort 值；无兄弟节点时返回 null */
  @Select(
      """
      SELECT MAX(sort) FROM organization
      WHERE (#{parentId} IS NULL AND parent_id IS NULL) OR parent_id = #{parentId}
      """)
  Integer selectMaxSiblingSort(@Param("parentId") String parentId);

  /**
   * 查某节点的相邻兄弟（上移取 sort 比它小的最大者，下移取 sort 比它大的最小者）。
   *
   * <p>{@code direction=up} 时按 sort 倒序取第一条，{@code down} 时正序取第一条。
   */
  @Select(
      """
      <script>
      SELECT id, name, type, parent_id AS parentId, path, sort
      FROM organization
      WHERE ((#{parentId} IS NULL AND parent_id IS NULL) OR parent_id = #{parentId})
        AND id != #{selfId}
        <choose>
          <when test="up">
            AND sort &lt; #{selfSort}
          </when>
          <otherwise>
            AND sort &gt; #{selfSort}
          </otherwise>
        </choose>
      ORDER BY
        <choose>
          <when test="up">sort DESC</when>
          <otherwise>sort ASC</otherwise>
        </choose>
      LIMIT 1
      </script>
      """)
  Organization selectAdjacentSibling(
      @Param("parentId") String parentId,
      @Param("selfId") String selfId,
      @Param("selfSort") int selfSort,
      @Param("up") boolean up);

  /** 同级是否存在同名节点（排除自身），用于重名校验 */
  @Select(
      """
      SELECT COUNT(*) FROM organization
      WHERE ((#{parentId} IS NULL AND parent_id IS NULL) OR parent_id = #{parentId})
        AND name = #{name}
        AND (#{excludeId} IS NULL OR id != #{excludeId})
      """)
  int countSiblingsWithName(
      @Param("parentId") String parentId,
      @Param("name") String name,
      @Param("excludeId") String excludeId);

  /**
   * 重命名时级联更新子孙的 path：把旧前缀替换为新前缀。
   *
   * <p><b>必须用 {@code CHAR_LENGTH} 而非 {@code LENGTH}</b> —— 后者返回字节数。 含中文的路径「33 个字符」等于「83 字节」，拿它当
   * SUBSTRING 起点会越界， 结果是子孙 path 被截断成父级 path（曾实际踩过这个坑）。
   */
  @Update(
      """
      UPDATE organization
      SET path = CONCAT(#{newPath}, SUBSTRING(path, CHAR_LENGTH(#{oldPath}) + 1))
      WHERE path LIKE CONCAT(#{oldPath}, '%') AND id != #{selfId}
      """)
  int replacePathPrefix(
      @Param("oldPath") String oldPath,
      @Param("newPath") String newPath,
      @Param("selfId") String selfId);
}
