# 西南大学「智畅游」README

## 仓库结构

* `android/`：Android 客户端（Gradle）
* `backend/`：Spring Boot 后端（Maven）
* `web-admin/`：Vue 管理后台（Vite）

## 版本关系说明
* **map1.0**：初始三端框架（Android + Web 管理后台 + Spring Boot），Android 地图基于 OSMDroid + 高德在线瓦片。
* **swu_gps**：在精简版前端上接入高德官方 Android SDK（3dmap-location-search），完成地图、定位、搜索、路线等核心能力。
* **map2.0**：以 map1.0 为基线，合并 swu_gps 的高德 SDK 关键代码，并在此基础上新增路线管理、分享社区、景点详情、触发历史、多图讲解点，**以及基于 RAG 架构的校园 AI 智能助手**等功能。

---

## 一、运行与关键说明

1. **数据库**
   * 请先运行 `backend/sql/mysql_full_setup.sql`
   * 再运行 `backend/sql/mysql_update_for_triggers_and_routes.sql`
   * 然后运行 `backend/sql/mysql_update_share_posts.sql`
   * 最后运行 `backend/sql/guide_location.sql`

2. **后端**
   * 复制 `backend/src/main/resources/application-local.properties.example` 为 `application-local.properties`，填写数据库账号密码与 DeepSeek API Key。
   * **注意**：请在该配置文件中一并配置好 DeepSeek-V3 的 API Key 以及 Vector DB 的连接信息，以保障 AI 问答引擎正常运转。
   * **注意**：后端一定设置 java 版本为 java17，否则会出现红色报错，但实际不影响运行。
   * 启动命令：在 `backend/` 目录执行 `mvn spring-boot:run`

3. **Web 管理端**
   * 请在 vscode 中打开，安装提示下载 vue 插件，否则会出现红色报错，但实际不影响运行。
   * 首次运行需要在 `web-admin/` 目录执行：`npm install`
   * 启动命令：`npm run dev`
   * 访问地址：http://localhost:5173/
   * **位置拾取注意**：管理端的经纬度请手动输入，不要在卫星地图中点击，坐标不准确。请通过高德坐标拾取器获取坐标（https://lbs.amap.com/tools/picker）。最好搜索后采用给出的坐标获取结果，鼠标点击显示的坐标结果注意不要输错，点击位置尽量靠近地点中心。如果安卓前端显示出现景点围栏偏移，请检查管理端对应的经纬度是否出错，基本都是经度 `106.×××`，纬度 `29.×××`。

4. **Android 前端**
   * **模拟器**：自动使用 `10.0.2.2:8091`（见 `AppConfig.java`）。
   * **真机**：修改 `android/app/src/main/res/values/strings.xml` 中 `backend_base_url` 为电脑局域网 IP。
   * **高德 Key**：如果高德应用的 key 无法使用，请自行在高德控制台进行注册获取新的 key（https://console.amap.com/dev/key/app）。
   * **实测建议**：模拟器容易闪退，建议真机实测。讲解功能说明：模拟器播放的声音比较快，并且声音不够清晰，但真机实际播放速度正常，播报清晰。

5. **模拟器定位（高德定位）**
   * 方式一：模拟器 Extended Controls（三个点）→ Location → 输入纬度、经度（例如 29.827800, 106.421900） → 点击 Set Location。
   * 方式二：如果 set location 为灰色不可点击状态，请通过 import GPX/KML 导入位置。`android/devtools/swu_campus_location.gpx` 为西南大学北碚校区校园中心，可以导入。

6. **功能说明**
   * 首次启动高德地图加载需等待片刻
   * 搜索地点时下方会提示「正在使用高德搜索」
   * 点击地图讲解点可查看详情，支持跳转路线规划
   * 底部「路线」支持步行/驾车/公交/打车，并展示途经地标与导航指引
   * 底部「分享」进入美食/住宿分享社区
   * 地图菜单「我的路线」可加载 Web 管理端保存的游览路线
   * **地图主页提供「AI 助手」悬浮入口，支持校园导览多轮智能对话。**

---

## 二、前端修改（Android `android/`）

### A. 自 swu_gps 合并的高德 SDK 改造（相对 map1.0）
* **`gradle/libs.versions.toml`**：新增高德地图组合 SDK（3dmap-location-search）依赖，统一管控高德 SDK 版本（amapSdk = 11.1.200_loc11.1.200_sea9.7.4）。
* **`CampusMapManager.java`**：由 OSMDroid 瓦片方案改为接入高德 Android 地图 SDK：地图初始化、校园边界多边形、图层切换（校园详图/卫星）、路线 Polyline 绘制、缩放与定位聚焦；默认锚点设为中心图书馆。
* **`MarkerManager.java`**：适配高德 Marker，实现讲解点与地理围栏渲染；支持标记点击弹窗、图标选中、信息展示及距离回调。
* **`gradle.properties`**：配置 Android 编译环境与 JDK 自动检测；注释掉硬编码 JBR 路径，便于不同开发机构建（相对 map1.0 更通用）。
* **`AppConfig.java`**：区分运行环境自动适配后端地址：模拟器使用 10.0.2.2:8091，真机读取 strings.xml 配置地址。
* **`SessionManager.java`**：新增后端地址存储、规整（`normalizeBaseUrl`）与同步能力，解决真机因地址错误引发的登录失败问题。
* **`app/build.gradle.kts`**：引入高德 SDK 依赖；配置 CPU 架构（arm64-v8a、armeabi-v7a）、原生库 useLegacyPackaging、Java 11 及 ViewBinding；新增 viewpager2 依赖；可选 copyAmapSdkToLibs 任务便于 IDE 索引。
* **`AndroidManifest.xml`**：添加网络、定位、WiFi 等权限；配置高德 Key、APSService 定位服务、明文网络（usesCleartextTraffic）、原生库解压（extractNativeLibs）、地图 Activity 硬件加速与 configChanges。
* **`strings.xml`**：更新后端接口地址、高德 Key；补充地图、路线、分享、登录、反馈、轨迹等业务中文文案。
* **`map_layout.xml`**：优化地图页面布局：高德 MapView、搜索栏、定位/缩放/图层工具栏、路线起终点 Spinner、路线模式 Chip、路线详情面板、底部导航（地图/景点/分享/我的）及服务快捷入口。
* **`MapActivity.java`**：重构地图核心逻辑：接入高德定位、POI 搜索、Inputtips、路线规划（步行/驾车/公交/打车）、轨迹记录；取消启动时强制自动定位；搜索与路线全面对接高德接口；集成分享入口与「我的路线」菜单。
* **`PoiInfoWindow.java`**：适配高德 Marker 信息窗展示逻辑。

### B. map2.0 在合并 swu_gps 基础上的新增与增强

**1. 地图模块架构拆分（相对 swu_gps 进一步重构）**
| 文件 | 说明 |
|---|---|
| `LocationService.java` | 封装高德 AMapLocationClient：隐私合规初始化、连续定位、生命周期管理 |
| `TrackManager.java` | 用户游览轨迹：记录定位点并用 Polyline 绘制 |
| `GeoFenceManager.java` | 地理围栏与自动讲解：距离判断、冷却防重复、本地围栏命中排序 |
| `AudioTriggerService.java` | 讲解音频触发：TTS 播报与 MediaPlayer 播放景点语音，从 MapActivity 抽离 |
| `PermissionUtil.java` | 定位等运行时权限统一申请 |
| `DistanceUtil.java` | 经纬度距离计算工具 |

**2. 路线规划与用户路线**
| 文件 | 说明 |
|---|---|
| `MapActivity.java`（增强） | 起终点 Spinner、路线模式切换、高德路线绘制、途经数据库景点高亮、加载已保存路线 |
| `RouteWaypointAdapter.java` | 路线途经点列表适配器 |
| `ApiService.java` | 新增 `/api/routes/` 路线规划与保存接口 |
| `RoutePlanDto.java` 等 DTO | 路线请求/响应数据传输对象 |
| `menu_map.xml` | 新增「我的路线」菜单项 |
| `strings.xml` | 路线相关文案（起终点、导航指引、我的路线提示等） |

**3. 景点详情与多图展示**
| 文件 | 说明 |
|---|---|
| `SpotDetailActivity.java` | 独立景点详情页：ViewPager2 多图轮播、分段讲解文案、跳转路线/导航 |
| `SpotImagePagerAdapter.java` | 详情页图片轮播适配器 |
| `SpotDetailTextUtil.java` | 讲解文案分段解析 |
| `LocationImageUrls.java` | 从 `imageUrl` / `imageUrls` 解析配图列表 |
| `SpotsIntroActivity.java` | 列表项点击进入 SpotDetailActivity |
| `SpotIntroAdapter.java` | 适配多图封面展示 |
| `LocationDto.java` | 新增 `imageUrls`、`cooldownSeconds` 字段 |
| `activity_spot_detail.xml` 等 | 详情页布局资源 |

**4. 分享社区（美食 / 住宿）**
| 文件 | 说明 |
|---|---|
| `ShareHubActivity.java` | 分享列表主页，分类切换（FOOD / HOTEL） |
| `ShareCreateActivity.java` | 发布/编辑分享帖，支持选图或标题渲染为配图 |
| `SharePostDetailActivity.java` | 帖子详情、点赞、评论 |
| `ShareImageViewerActivity.java` | 全屏查看分享图片 |
| `MyShareActivity.java` | 个人中心「我的分享」 |
| `SharePostListAdapter.java` | 分享列表适配器 |
| `SharePostDto.java` 等 DTO | 分享帖/评论数据传输对象 |
| `ApiService.java` | 新增 `/api/share/*` 接口 |
| `ProfileActivity.java` | 增加「我的分享」入口 |
| `map_layout.xml` | 底部「分享」Tab、美食/住宿服务快捷按钮 |
| `activity_share_.xml` 等 | 分享相关布局 |

**5. 触发历史**
| 文件 | 说明 |
|---|---|
| `TriggerHistoryActivity.java` | 查看某讲解点的围栏/讲解触发记录 |
| `TriggerHistoryAdapter.java` | 触发历史列表适配器 |
| `TriggerHistoryDto.java` | 触发历史 DTO |
| `ApiService.java` | 新增 `GET /api/locations/{id}/trigger-history` |

**6. AI 智能助手与网络长连优化**
| 文件 | 说明 |
|---|---|
| `AiAssistantActivity.java` | 沉浸式 AI 交互界面，支持长文本流式响应与 16 轮以上历史上下文记忆留存。 |
| `RetrofitClient.java` | 重构网络通信层，**针对大模型推理专门将连接/读取/写入超时全链路提升至 60 秒**，彻底解决高并发长连断线痛点。 |
| `AiChatAdapter.java` | 聊天气泡列表适配器，区分用户提问与大模型回复 UI。 |
| `AiMessageDto.java` 等 | 智能助手通信模型封装。 |

**7. 其他前端改动**
| 文件 | 说明 |
|---|---|
| `AdminActivity.java` | 适配多图字段与讲解点维护 |
| `LocationUpsertReq.java` | 管理端上传字段扩展 |
| `LoginActivity.java` | 登录流程与后端地址同步优化 |
| `DistanceFormat.java` | 距离展示格式优化 |
| `colors.xml` | 分享、路线等 UI 配色补充 |
| `RecyclerViewHeightUtil.java` | 评论列表高度自适应 |

---

## 三、后端（Spring Boot `backend/`）

### A. 自 swu_gps 合并说明
swu_gps 的后端修改记录中列出的 `build.gradle.kts`、`AndroidManifest.xml`、`activity_map.xml`、`MapActivity.java` 实际属于 Android 前端，后端在 swu_gps 中无实质地图 SDK 改动。map2.0 后端仍以 map1.0 的 Spring Boot 工程为基础扩展。

### B. map2.0 后端新增模块

**1. 路线规划与用户路线**
| 文件 | 说明 |
|---|---|
| `RouteController.java` | REST：`/api/routes/plan`、`plan-multi-stop`、`my-routes`、`save`、`/{id}`、`DELETE` |
| `RoutePlanningService.java` | 两点/多景点路线规划（距离估算、途经点排序）、用户路线 CRUD |
| `UserRoute.java` / `RouteWaypoint.java` | 用户保存路线及途经点实体 |
| `UserRouteRepository.java` | 路线持久化 |
| `RoutePlanRequest.java` 等 | 请求/响应模型 |

**2. 分享社区**
| 文件 | 说明 |
|---|---|
| `SharePostController.java` | REST：`/api/share/posts`、`comments`、`like`、图片上传 |
| `SharePostService.java` | 帖子 CRUD、评论、点赞、标题转配图（`TitleImageGenerator`） |
| `SharePost.java` / `SharePostComment.java` / `SharePostLike.java` | 分享帖实体 |
| `ShareCategory.java` | 分类枚举（FOOD / HOTEL） |
| `SharePostRepository.java` 等 | 数据访问层 |
| `TitleImageGenerator.java` | 将帖子标题渲染为分享配图 |

**3. 讲解点多图与媒体**
| 文件 | 说明 |
|---|---|
| `GuideLocation.java` | 新增 `imageUrlsJson` 字段，支持多张配图 JSON 存储 |
| `ImageUrlsJsonUtil.java` | 多图 URL JSON 序列化/反序列化 |
| `LocationMediaController.java` | 管理端：`POST/DELETE /api/admin/locations/{id}/images`、`audio` |
| `LocationMediaService.java` | 讲解点配图/语音增删逻辑 |
| `LocationResponse.java` 等 | 响应中返回 `imageUrls` 列表 |

**4. 触发历史**
| 文件 | 说明 |
|---|---|
| `TriggerHistory.java` | 用户围栏/讲解触发记录实体 |
| `TriggerHistoryRepository.java` | 按用户、讲解点查询触发历史 |
| `LocationController.java` | 新增 `GET /api/locations/{id}/trigger-history`；打卡时写入触发记录 |
| `LocationService.java` | `recordTriggerHistory`、`getUserTriggerHistory` |
| `TriggerHistoryResponse.java` | 触发历史响应模型 |

**5. RAG 智能问答引擎 (AI 助手)**
| 文件 | 说明 |
|---|---|
| `RagChatController.java` | REST：`/api/ai/chat` 处理前端多轮问答对话请求。 |
| `RagEngineService.java` | 封装 RAG 核心流水线：意图九分类识别、Vector DB 相似度检索召回、以及**专知优先/百科兜底**的双态 Prompt 动态注入逻辑。 |
| `DeepSeekClient.java` | 深度集成 DeepSeek-V3 API，负责大模型云端推理与文本生成回调。 |
| `KnowledgeSlice.java` | 知识库切片实体，用于 RAG 检索增强。 |

**6. 其他后端改动（相对 map1.0 增强）**
| 文件 | 说明 |
|---|---|
| `DataInitializer.java` | 种子数据适配多图、分享分类等 |
| `WebConfig.java` | 静态资源与 CORS 配置更新 |
| `PublicMediaController.java` / `MediaService.java` | 媒体访问与分享图片存储路径扩展 |
| `LocationService.java` / `AdminService.java` | 讲解点业务逻辑适配多图、冷却时间 |
| `AuthService.java` / `AuthInterceptor.java` | 鉴权与分享/路线接口权限 |
| `RagCategory.java` 等 | AI 知识库分类微调 |

---

## 四、Web 管理后台（`web-admin/`）
| 文件 | 说明 |
|---|---|
| `RoutesView.vue` | 路线列表：查看、删除用户保存路线 |
| `RouteFormView.vue` | 新建多景点游览路线 |
| `RouteDetailView.vue` | 路线详情与途经点展示 |
| `router/index.ts` | 注册 `/routes`、`/routes/new`、`/routes/:id` 路由 |
| `AdminLayout.vue` | 侧边栏增加「路线管理」入口 |
| `api/admin.ts` | 路线 CRUD、多图上传 API 封装 |
| `types/api.ts` | 路线、多图相关 TypeScript 类型 |
| `LocationFormView.vue` | 讲解点多图上传与管理 |
| `LocationsView.vue` / `DashboardView.vue` | 列表与统计展示适配 |
| `LocationMapPicker.vue` | 地图选点组件优化 |
| `RagKnowledgeView.vue` | **(新增)** RAG 专属文档 CRUD 与知识库语义切片可视化维护界面。 |

---

## 五、与 map1.0 / swu_gps 差异速查
| 维度 | map1.0 | swu_gps | map2.0 |
|---|---|---|---|
| **地图引擎** | OSMDroid + 高德瓦片 | 高德官方 SDK | 高德官方 SDK（合并自 swu_gps） |
| **三端完整性** | Android + Web + 后端 | 以前端为主 | 完整三端 |
| **路线规划** | 前端简易估算 | 高德路线原型 | 高德路线 + 后端保存 + Web 管理 |
| **分享社区** | 无 | 无 | 美食/住宿分享帖 + 评论点赞 |
| **景点详情** | 列表页内展示 | 列表页内展示 | 独立详情页 + 多图轮播 |
| **触发历史** | 无 | 无 | 前后端完整支持 |
| **讲解点多图** | 单张 `imageUrl` | 单张 | `imageUrlsJson` 多图 |
| **AI 问答助手** | 无 | 无 | **接入 DeepSeek-V3，基于 RAG 架构的垂直专知问答** |
| **代码结构** | MapActivity 单体 | MapActivity 单体 | 定位/围栏/音频/轨迹/AI问答模块化 |