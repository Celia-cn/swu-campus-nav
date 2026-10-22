# 西南大学智畅游 · Web 管理后台

与 Android 客户端 `android` 共用后端 API（`backend`，默认 `http://localhost:8091`）。

## 功能

- 管理员登录（仅 `ADMIN` 角色）
- 数据统计：今日访问人数、热门景点、讲解播放次数等
- 讲解点管理：新增 / 编辑 / 删除，支持上传图片与语音
- 用户管理：查看注册用户与角色

## 开发

```bash
cd web-admin
npm install
npm run dev
```

浏览器访问 http://localhost:5173 。Vite 已将 `/api` 与 `/media` 代理到后端。

## 默认管理员账号

与后端初始化数据一致：

- 用户名：`admin`
- 密码：`admin123456`

## 生产构建

```bash
npm run build
```

将 `dist` 目录部署到任意静态服务器，并确保能访问后端 API（或配置反向代理）。
