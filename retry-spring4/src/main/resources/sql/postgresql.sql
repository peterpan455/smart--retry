create table sys_retry_task (
task_id serial not null primary key,
identity_name varchar(50) not null,
params text,
status smallint not null,
retry_count int not null default 0,
remark varchar(1000),
create_date timestamp not null,
edit_date timestamp);

comment on column sys_retry_task.identity_name is '任务的唯一标识';
comment on column sys_retry_task.params is '参数';
comment on column sys_retry_task.status is '状态。1: 处理中，2: 成功，3: 失败';
comment on column sys_retry_task.retry_count is '重试次数';
comment on column sys_retry_task.remark is '备注';
comment on table sys_retry_task is '系统重试表';

create index idx_identityname_status ON sys_retry_task(identity_name asc,status asc);