-- =============================================
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
CREATE TABLE roles (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200)
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
CREATE TABLE user_experiments (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    template_id BINARY(16) NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    total_duration INT DEFAULT 0,
    status TINYINT DEFAULT 0 COMMENT '0进行中 1完成',
    score DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (template_id) REFERENCES experiment_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户实验步骤记录表
CREATE TABLE user_experiment_steps (
    id BINARY(16) PRIMARY KEY,
    experiment_id BINARY(16) NOT NULL,
    step_id BINARY(16) NOT NULL,
    status TINYINT DEFAULT 0,
    duration_seconds INT DEFAULT 0,
    operation_count INT DEFAULT 0,
    error_count INT DEFAULT 0,
    score DECIMAL(5,2),
    result_data JSON,
                                       started_at DATETIME,
                                       finished_at DATETIME,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (experiment_id) REFERENCES user_experiments(id),
                                       FOREIGN KEY (step_id) REFERENCES experiment_steps(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;