package com.example.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.experiment.dto.admin.UserDetailVO;
import com.example.experiment.dto.admin.UserListVO;
import com.example.experiment.dto.admin.UserQueryDTO;
import com.example.experiment.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UsersMapper extends BaseMapper<Users> {

  /**
   * 按角色层级分页查询可见用户。
   *
   * <p>可见范围 = 用户的最高角色 level {@code <} 当前用户的 maxLevel。取 MAX 是因为一个人可能持有多个角色， 直接 JOIN
   * 会让多角色用户在列表里出现重复行；取最高层级再比较，语义才是「这个人是否比我低」。
   *
   * <p>动态条件用 {@code <script>} 包裹：MyBatis 注解 SQL 中写 {@code <if>} 必须如此。
   *
   * <p>{@code orgId} 的过滤走 {@code path LIKE 前缀%}，覆盖该节点及其所有子节点。
   */
  @Select(
      """
      <script>
      SELECT u.id       AS id,
             u.username AS username,
             u.name     AS name,
             u.gender   AS gender,
             u.phone    AS phone,
             o.path     AS orgName
      FROM users u
      LEFT JOIN organization o ON o.id = u.org_id
      WHERE (
              SELECT MAX(r.level)
              FROM user_roles ur
              JOIN roles r ON r.id = ur.role_id
              WHERE ur.user_id = u.id
            ) &lt; #{maxLevel}
        <if test="q.name != null and q.name != ''">
          AND u.name LIKE CONCAT('%', #{q.name}, '%')
        </if>
        <if test="q.username != null and q.username != ''">
          AND u.username LIKE CONCAT('%', #{q.username}, '%')
        </if>
        <if test="q.phone != null and q.phone != ''">
          AND u.phone LIKE CONCAT('%', #{q.phone}, '%')
        </if>
        <if test="q.orgId != null and q.orgId != ''">
          AND o.path LIKE CONCAT((SELECT p.path FROM organization p WHERE p.id = #{q.orgId}), '%')
        </if>
      ORDER BY u.created_at DESC, u.id
      </script>
      """)
  IPage<UserListVO> selectVisibleUsers(
      Page<UserListVO> page, @Param("q") UserQueryDTO query, @Param("maxLevel") int maxLevel);

  /**
   * 查询单个用户的详情（含组织路径）。
   *
   * <p>此处不做层级过滤 —— 调用方先校验目标是否可见，越权时返回 403 而非 404， 便于前端区分「无权操作」与「用户不存在」。
   */
  @Select(
      """
      SELECT u.id         AS id,
             u.username   AS username,
             u.name       AS name,
             u.gender     AS gender,
             u.birth_date AS birthday,
             u.student_no AS studentNo,
             u.phone      AS phone,
             u.email      AS email,
             u.created_at AS createdAt,
             o.path       AS orgName
      FROM users u
      LEFT JOIN organization o ON o.id = u.org_id
      WHERE u.id = #{userId}
      """)
  UserDetailVO selectUserDetail(@Param("userId") String userId);
}
