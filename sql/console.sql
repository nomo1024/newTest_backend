create table 实训作业.gps_data
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    col1         double                             null comment '数据1',
    collect_time datetime default CURRENT_TIMESTAMP null comment '采集时间'
)
    comment 'GPS数据表' charset = utf8mb4;

create index idx_collect_time
    on 实训作业.gps_data (collect_time);

create table 实训作业.humidity_data
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    col1         double                             null comment '数据1',
    collect_time datetime default CURRENT_TIMESTAMP null comment '采集时间'
)
    comment '湿度数据表' charset = utf8mb4;

create index idx_collect_time
    on 实训作业.humidity_data (collect_time);

create table 实训作业.light_data
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    col1         double                             null comment '数据1',
    collect_time datetime default CURRENT_TIMESTAMP null comment '采集时间'
)
    comment '光照数据表' charset = utf8mb4;

create index idx_collect_time
    on 实训作业.light_data (collect_time);

create table 实训作业.pressure_data
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    col1         double                             null comment '数据1',
    collect_time datetime default CURRENT_TIMESTAMP null comment '采集时间'
)
    comment '气压数据表' charset = utf8mb4;

create index idx_collect_time
    on 实训作业.pressure_data (collect_time);

create table 实训作业.temperature_data
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    col1         double                             null comment '数据1',
    collect_time datetime default CURRENT_TIMESTAMP null comment '采集时间'
)
    comment '温度数据表' charset = utf8mb4;

create index idx_collect_time
    on 实训作业.temperature_data (collect_time);

create table 实训作业.user
(
    id           int auto_increment
        primary key,
    userAccount  varchar(256)                       not null comment '用户名',
    userPassword varchar(512)                       not null comment '用户密码',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    userRole     int      default 0                 null comment '用户身份',
    email        varchar(256)                       null comment '邮箱',
    phone        varchar(256)                       null comment '电话',
    userStatus   int                                null comment '用户状态',
    isDelete     tinyint  default 0                 not null comment '是否删除'
)
    comment '用户表';

