-- 用户表增量列（PostgreSQL）
-- 使用 DO 块实现幂等的 ADD COLUMN。

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user' AND column_name = 'must_change_password') THEN
        ALTER TABLE "user" ADD COLUMN must_change_password INTEGER DEFAULT 0;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user' AND column_name = 'created_by') THEN
        ALTER TABLE "user" ADD COLUMN created_by VARCHAR(64);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user' AND column_name = 'updated_by') THEN
        ALTER TABLE "user" ADD COLUMN updated_by VARCHAR(64);
    END IF;
END $$;
