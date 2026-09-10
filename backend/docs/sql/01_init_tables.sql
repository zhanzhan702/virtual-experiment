-- V1 初始建表脚本
-- 组织架构表
CREATE TABLE organization (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'university/college/major/grade',
    parent_id BINARY(16) DEFAULT NULL,
    path VARCHAR(500) DEFAULT NULL,
    sort INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户表
CREATE TABLE users (
    id BINARY(16) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    name VARCHAR(50) NOT NULL,
    gender TINYINT DEFAULT 0,
    birth_date DATE,
    student_no VARCHAR(50),
    org_id BINARY(16) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (org_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 角色表
-- level：角色层级，数值越大权限越高；用户可见范围 = level < 当前用户的 level
CREATE TABLE roles (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    level INT NOT NULL DEFAULT 0 COMMENT '角色层级，数值越大权限越高'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户角色关联表
CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 实验模板表
CREATE TABLE experiment_templates (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL COMMENT '模板编码',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    category VARCHAR(30) NOT NULL COMMENT 'high_voltage / low_voltage',
    mode VARCHAR(20) NOT NULL DEFAULT 'training' COMMENT 'training / exam',
    version VARCHAR(20) DEFAULT '1.0' COMMENT '模板版本',
    description TEXT COMMENT '模板描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 实验步骤表
CREATE TABLE experiment_steps (
    id BINARY(16) PRIMARY KEY,
    template_id BINARY(16) NOT NULL,
    step_order INT NOT NULL,
    step_code VARCHAR(50) NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    required_seconds INT DEFAULT 0,
    score DECIMAL(5,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES experiment_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户实验记录表
-- draft_data：当前存档步骤草稿（saveDraft 写入，submitStep 清空）
-- ticket_data：工作票提交数据（ticketNo + member1，submitStep 写入，不清空）
-- score：系统算出的原始加权分 = Σ 步骤模板score × 得分率，量纲等于该模板步骤分合计
-- manual_score：教师人工改分，**百分制**（0-100）。与 score 量纲不同是刻意的 ——
--               模板步骤分值后期会调整，存百分制可避免历史改分含义漂移
CREATE TABLE user_experiments (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    template_id BINARY(16) NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    total_duration INT DEFAULT 0,
    status TINYINT DEFAULT 0 COMMENT '0进行中 1完成',
    score DECIMAL(5,2) COMMENT '系统原始加权分（量纲=模板步骤分合计）',
    manual_score DECIMAL(5,2) COMMENT '人工改分（百分制 0-100）',
    scored_by BINARY(16) COMMENT '改分人',
    scored_at DATETIME COMMENT '改分时间',
    draft_data JSON COMMENT '当前存档步骤草稿（saveDraft写入，submitStep清空）',
    ticket_data JSON COMMENT '工作票提交数据（ticketNo+member1）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (template_id) REFERENCES experiment_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户实验步骤记录表（result_data 已移除：草稿/工作票数据存 user_experiments）
CREATE TABLE user_experiment_steps (
    id BINARY(16) PRIMARY KEY,
    experiment_id BINARY(16) NOT NULL,
    step_id BINARY(16) NOT NULL,
    status TINYINT DEFAULT 0,
    duration_seconds INT DEFAULT 0,
    operation_count INT DEFAULT 0,
    error_count INT DEFAULT 0,
    score DECIMAL(5,2),
    started_at DATETIME,
    finished_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (experiment_id) REFERENCES user_experiments(id),
    FOREIGN KEY (step_id) REFERENCES experiment_steps(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
