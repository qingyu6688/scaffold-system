-- 会话版本与账号变更在同一事务提交，避免并发登录绕过撤销。
ALTER TABLE sys_user ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0 COMMENT '会话版本，敏感变更后递增';
