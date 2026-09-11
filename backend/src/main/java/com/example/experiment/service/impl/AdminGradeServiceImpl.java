package com.example.experiment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.experiment.dto.admin.ClassSummaryVO;
import com.example.experiment.dto.admin.StudentExperimentVO;
import com.example.experiment.dto.admin.StudentGradeVO;
import com.example.experiment.dto.admin.UserListVO;
import com.example.experiment.dto.admin.UserScoreRowVO;
import com.example.experiment.entity.Organization;
import com.example.experiment.entity.UserExperiments;
import com.example.experiment.exception.ApiException;
import com.example.experiment.mapper.OrganizationMapper;
import com.example.experiment.mapper.UserExperimentsMapper;
import com.example.experiment.mapper.UsersMapper;
import com.example.experiment.service.AdminGradeService;
import com.example.experiment.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminGradeServiceImpl implements AdminGradeService {

  private static final String HIGH = "high_voltage";
  private static final String LOW = "low_voltage";

  /** 班级汇总时单班一次取回的学生上限。见 loadStudentsInClasses 的说明 */
  private static final int STUDENTS_PER_CLASS_LIMIT = 500;

  private final OrganizationMapper organizationMapper;
  private final UsersMapper usersMapper;
  private final UserExperimentsMapper userExperimentsMapper;
  private final UserService userService;

  // ──────────────────────── 班级成绩汇总 ────────────────────────

  @Override
  public List<ClassSummaryVO> getClassSummaries(String orgId, int myMaxLevel) {
    // 一次性取回全部组织节点：既用于筛出子树，也用于拼接班级名时的向上回溯
    // （班级的父级可能在所选子树之外，比如选中年级时班级的学院/学校不在子树里）
    List<Organization> allOrgs = organizationMapper.selectAllOrdered();
    Map<String, Organization> orgById =
        allOrgs.stream().collect(Collectors.toMap(Organization::getId, Function.identity()));

    List<Organization> classes =
        loadSubtree(orgId, allOrgs).stream()
            .filter(o -> "class".equals(o.getType()))
            .collect(Collectors.toList());

    if (classes.isEmpty()) {
      // 该节点下没有班级（例如空年级）
      return List.of();
    }

    // 一次取回这些班级下的全部学生，避免逐班查库
    List<UserListVO> students =
        loadStudentsInClasses(classes.stream().map(Organization::getId).toList(), myMaxLevel);

    Map<String, List<UserListVO>> studentsByClass =
        students.stream()
            .filter(s -> s.getOrgId() != null)
            .collect(Collectors.groupingBy(UserListVO::getOrgId));

    Map<String, Map<String, BestScore>> scoresByUser =
        loadBestScoresByUser(students.stream().map(UserListVO::getId).toList());

    List<ClassSummaryVO> result = new ArrayList<>();
    for (Organization cls : classes) {
      List<UserListVO> members = studentsByClass.getOrDefault(cls.getId(), List.of());

      int highDone = 0;
      int lowDone = 0;
      List<BigDecimal> validScores = new ArrayList<>();

      for (UserListVO student : members) {
        Map<String, BestScore> byCategory = scoresByUser.get(student.getId());
        if (byCategory == null) {
          continue;
        }
        BestScore high = byCategory.get(HIGH);
        BestScore low = byCategory.get(LOW);
        if (high != null) {
          highDone++;
          validScores.add(high.score());
        }
        if (low != null) {
          lowDone++;
          validScores.add(low.score());
        }
      }

      ClassSummaryVO vo = new ClassSummaryVO();
      vo.setOrgId(cls.getId());
      vo.setClassName(buildClassName(cls, orgById));
      vo.setOrgName(cls.getPath());
      vo.setStudentCount(members.size());
      vo.setHighDone(highDone);
      vo.setLowDone(lowDone);
      vo.setAvgScore(average(validScores));
      result.add(vo);
    }
    return result;
  }

  // ───────────────────────── 学生成绩 ─────────────────────────

  @Override
  public IPage<StudentGradeVO> getStudentGrades(
      String orgId, String name, int page, int size, int myMaxLevel) {

    String pathPrefix = resolvePathPrefix(orgId);

    Page<UserListVO> pager = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 200));
    IPage<UserListVO> studentPage =
        usersMapper.selectStudentsUnderOrg(pager, pathPrefix, name, myMaxLevel);

    Map<String, Map<String, BestScore>> scoresByUser =
        loadBestScoresByUser(studentPage.getRecords().stream().map(UserListVO::getId).toList());

    List<StudentGradeVO> rows = new ArrayList<>();
    for (UserListVO student : studentPage.getRecords()) {
      Map<String, BestScore> byCategory = scoresByUser.getOrDefault(student.getId(), Map.of());

      StudentGradeVO vo = new StudentGradeVO();
      vo.setUserId(student.getId());
      vo.setUsername(student.getUsername());
      vo.setName(student.getName());
      applyCategory(byCategory.get(HIGH), true, vo);
      applyCategory(byCategory.get(LOW), false, vo);
      rows.add(vo);
    }

    // 用同一个 Page 承载当前页信息，避免前端因类型不同而拿不到 total
    Page<StudentGradeVO> result = new Page<>(studentPage.getCurrent(), studentPage.getSize());
    result.setTotal(studentPage.getTotal());
    result.setRecords(rows);
    return result;
  }

  @Override
  public List<StudentExperimentVO> getStudentExperiments(String userId, int myMaxLevel) {
    requireVisibleStudent(userId, myMaxLevel);

    return userExperimentsMapper.selectStudentRows(userId).stream()
        .map(
            row -> {
              StudentExperimentVO vo = new StudentExperimentVO();
              vo.setExperimentId(row.getExperimentId());
              vo.setTemplateName(row.getTemplateName());
              vo.setCategory(row.getCategory());
              vo.setStatus(row.getStatus());
              vo.setRawScore(row.getRawScore());
              vo.setPercentScore(toPercent(row.getRawScore(), row.getStepTotal()));
              vo.setManualScore(row.getManualScore());
              vo.setStartTime(row.getStartTime());
              vo.setEndTime(row.getEndTime());
              vo.setTotalDuration(row.getTotalDuration());
              vo.setScoredAt(row.getScoredAt());
              return vo;
            })
        .collect(Collectors.toList());
  }

  // ─────────────────────────── 改分 ───────────────────────────

  @Override
  @Transactional
  public void updateScore(
      String experimentId, BigDecimal manualScore, String operatorId, int myMaxLevel) {

    UserExperiments experiment = userExperimentsMapper.selectById(experimentId);
    if (experiment == null) {
      throw ApiException.notFound("实验记录不存在");
    }
    requireVisibleStudent(experiment.getUserId(), myMaxLevel);

    // 前端已限制 0-100，服务端再校验一次（DTO 上的 @DecimalMin/@DecimalMax 只覆盖非空值）
    if (manualScore != null
        && (manualScore.compareTo(BigDecimal.ZERO) < 0
            || manualScore.compareTo(BigDecimal.valueOf(100)) > 0)) {
      throw ApiException.badRequest("分数需在 0-100 之间");
    }

    // 用 lambdaUpdate 而非 updateById：后者默认忽略 null 字段，
    // 而「撤销改分」恰恰要把 manual_score / scored_by / scored_at 显式置回 NULL
    userExperimentsMapper.update(
        null,
        Wrappers.<UserExperiments>lambdaUpdate()
            .eq(UserExperiments::getId, experimentId)
            .set(UserExperiments::getManualScore, manualScore)
            .set(UserExperiments::getScoredBy, manualScore == null ? null : operatorId)
            .set(UserExperiments::getScoredAt, manualScore == null ? null : LocalDateTime.now()));
  }

  // ────────────────────────── 内部工具 ──────────────────────────

  /** 一个学生在一个类别下的最优成绩 */
  private record BestScore(BigDecimal score, boolean manual) {}

  /**
   * 把原始加权分换算成百分制。
   *
   * <p>分母来自模板步骤分合计，实时求和而非硬编码 —— 模板步骤分值调整后历史分数自动跟随。 分母为 0 或缺失时返回 null（表示无法换算），而不是抛异常或返回 0。
   */
  private BigDecimal toPercent(BigDecimal raw, BigDecimal stepTotal) {
    if (raw == null || stepTotal == null || stepTotal.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return raw.multiply(BigDecimal.valueOf(100)).divide(stepTotal, 1, RoundingMode.HALF_UP);
  }

  /**
   * 批量取每个学生在每个类别下的最终展示成绩。
   *
   * <p>规则（人工改分优先）：
   *
   * <ol>
   *   <li>该类别下只要有任意一条记录被人工改分，就取人工分中的最高值 —— 教师改高的意图必须生效
   *   <li>否则取各次尝试换算成百分制后的最高值
   * </ol>
   *
   * <p>为什么不是把人工分和系统分放在一起比大小：那样教师把 95.6 改成 85 时， 学生另一次尝试的 91.3 会把这个 85 顶掉，界面看起来像「改分没生效」。
   */
  private Map<String, Map<String, BestScore>> loadBestScoresByUser(List<String> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }

    // 先按 用户 → 类别 归集，再逐组决策
    Map<String, Map<String, List<UserScoreRowVO>>> grouped = new HashMap<>();
    for (UserScoreRowVO row : userExperimentsMapper.selectScoreRows(userIds)) {
      grouped
          .computeIfAbsent(row.getUserId(), k -> new HashMap<>())
          .computeIfAbsent(row.getCategory(), k -> new ArrayList<>())
          .add(row);
    }

    Map<String, Map<String, BestScore>> result = new HashMap<>();
    grouped.forEach(
        (userId, byCategory) -> {
          Map<String, BestScore> perUser = new HashMap<>();
          byCategory.forEach(
              (category, rows) -> {
                BestScore best = pickBest(rows);
                if (best != null) {
                  perUser.put(category, best);
                }
              });
          result.put(userId, perUser);
        });
    return result;
  }

  /** 在同一个「学生 + 类别」的多条记录中挑出展示值 */
  private BestScore pickBest(List<UserScoreRowVO> rows) {
    BigDecimal bestManual =
        rows.stream()
            .map(UserScoreRowVO::getManualScore)
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(null);

    if (bestManual != null) {
      return new BestScore(bestManual, true);
    }

    return rows.stream()
        .map(row -> toPercent(row.getRawScore(), row.getStepTotal()))
        .filter(Objects::nonNull)
        .max(BigDecimal::compareTo)
        .map(score -> new BestScore(score, false))
        .orElse(null);
  }

  /** 把最优成绩填入 VO 的高压或低压字段 */
  private void applyCategory(BestScore best, boolean high, StudentGradeVO vo) {
    boolean done = best != null;
    BigDecimal score = best == null ? null : best.score();
    boolean manual = best != null && best.manual();

    if (high) {
      vo.setHighDone(done);
      vo.setHighScore(score);
      vo.setHighManual(manual);
    } else {
      vo.setLowDone(done);
      vo.setLowScore(score);
      vo.setLowManual(manual);
    }
  }

  private BigDecimal average(List<BigDecimal> values) {
    if (values.isEmpty()) {
      return null;
    }
    BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
  }

  /** 取某节点的 path 前缀；orgId 为空时返回根前缀（即全部） */
  private String resolvePathPrefix(String orgId) {
    if (orgId == null || orgId.isBlank()) {
      return "/";
    }
    Organization org = organizationMapper.selectById(orgId);
    if (org == null || org.getPath() == null) {
      throw ApiException.notFound("组织节点不存在");
    }
    return org.getPath();
  }

  /**
   * 从全部节点中筛出某节点及其子孙；orgId 为空时返回全部。
   *
   * <p>复用已取回的 {@code allOrgs} 而非再查一次库 —— 用 path 前缀判断祖先关系。
   */
  private List<Organization> loadSubtree(String orgId, List<Organization> allOrgs) {
    if (orgId == null || orgId.isBlank()) {
      return allOrgs;
    }
    Organization org =
        allOrgs.stream().filter(o -> o.getId().equals(orgId)).findFirst().orElse(null);
    if (org == null || org.getPath() == null) {
      throw ApiException.notFound("组织节点不存在");
    }
    return allOrgs.stream()
        .filter(o -> o.getPath() != null && o.getPath().startsWith(org.getPath()))
        .collect(Collectors.toList());
  }

  /**
   * 拼接班级完整名，如「电气工程及其自动化2024级1班」。
   *
   * <p>库里班级名只有「1班」，脱离上下文无法辨识，故向上回溯父级名称拼接，参照原仿真后台的命名风格。
   *
   * <p>拼接**止于专业层**：学院与学校对同一学院下的所有班级都相同，放进班级名既冗余又冗长
   * （会得到「物理与电子信息工程学院电气工程及其自动化2024级1班」）。原站的班级名也只含年级与专业。
   */
  private String buildClassName(Organization cls, Map<String, Organization> allById) {
    List<String> names = new ArrayList<>();
    Organization cursor = cls;
    while (cursor != null) {
      String type = cursor.getType();
      if ("university".equals(type) || "college".equals(type)) {
        break;
      }
      names.add(0, cursor.getName());
      cursor = cursor.getParentId() == null ? null : allById.get(cursor.getParentId());
    }
    return String.join("", names);
  }

  /**
   * 取这些班级下的全部学生（含无实验记录的），已按层级过滤。
   *
   * <p>班级路径彼此不重叠（每个班级只在一条路径末端），故逐个按 path 前缀查不会重复取到同一学生。
   *
   * <p><b>已知限制</b>：单个班级一次最多取 {@value #STUDENTS_PER_CLASS_LIMIT} 人， 超出部分会被静默丢弃 ——
   * 班级汇总的人数与完成数随之偏小。正常教学班级远达不到这个量， 若将来出现超大规模班级，需改为在 SQL 侧直接聚合而不是取回全部学生再在内存里算。
   */
  private List<UserListVO> loadStudentsInClasses(
      java.util.Collection<String> classIds, int myMaxLevel) {

    if (classIds.isEmpty()) {
      return List.of();
    }
    List<UserListVO> all = new ArrayList<>();
    for (String classId : classIds) {
      Organization cls = organizationMapper.selectById(classId);
      if (cls == null || cls.getPath() == null) {
        continue;
      }
      Page<UserListVO> pager = new Page<>(1, STUDENTS_PER_CLASS_LIMIT);
      IPage<UserListVO> page =
          usersMapper.selectStudentsUnderOrg(pager, cls.getPath(), null, myMaxLevel);
      all.addAll(page.getRecords());
    }
    return all;
  }

  /** 目标学生必须存在且在操作者可见范围内（层级低于操作者） */
  private void requireVisibleStudent(String userId, int myMaxLevel) {
    if (userService.findById(userId) == null) {
      throw ApiException.notFound("用户不存在");
    }
    if (userService.getMaxLevel(userId) >= myMaxLevel) {
      throw ApiException.forbidden("无权查看该学生的成绩");
    }
  }
}
