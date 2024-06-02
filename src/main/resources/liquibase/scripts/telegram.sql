-- liquibase formatted sql

-- changeset dkhan:1

CREATE TABLE notification_task(
id BIGSERIAL primary key,
chat_id BIGINT,
task_text VARCHAR,
task_clock TIMESTAMP
)