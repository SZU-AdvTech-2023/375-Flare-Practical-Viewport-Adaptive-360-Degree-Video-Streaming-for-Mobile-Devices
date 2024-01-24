# 介绍

MC2-BLOCK是一个分块传输平台，旨在为各类传输算法提供基础的运行环境

# 项目文件结构

```shell
.
├── client 客户端相关实现
│   └── tmp (可删除)
├── doc 
│   ├── Client.md  部署文档
│   ├── Interface.md 接口文档
│   └── Server.md 部署文档
├── README.md
└── server 服务端相关实现
    └── tmp (可删除)

```

# Git Commit 提交规范

```
`<type>`(`<scope>`): `<subject>`
```

1. type(必须)
   feat：新功能
   fix/to：修复bug
   fix：修复了该问题
   to：正在修复，但还没修完
   docs：文档改动
   merge：代码合并
2. scope 用于说明 commit 影响的范围
3. subject 用于说明做了什么事
4. 例子
```
    fix(DAO):用户查询缺少username属性
    feat(Controller):用户查询接口开发
    to(DAO):属性错误修改
    docs(DAO):修改连接接口说明
```
