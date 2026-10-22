import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/http'
import { fetchMe } from '@/api/admin'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '数据统计' },
        },
        {
          path: 'locations',
          name: 'locations',
          component: () => import('@/views/LocationsView.vue'),
          meta: { title: '讲解点管理' },
        },
        {
          path: 'locations/new',
          name: 'location-create',
          component: () => import('@/views/LocationFormView.vue'),
          meta: { title: '新增讲解点' },
        },
        {
          path: 'locations/:id/edit',
          name: 'location-edit',
          component: () => import('@/views/LocationFormView.vue'),
          meta: { title: '编辑讲解点' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UsersView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'feedback',
          name: 'feedback',
          component: () => import('@/views/FeedbackView.vue'),
          meta: { title: '反馈中心' },
        },
        {
          path: 'rag',
          name: 'rag',
          component: () => import('@/views/RagView.vue'),
          meta: { title: 'AI 知识库' },
        },

        // 路线管理相关路由
        {
          path: 'routes',
          name: 'Routes',
          component: () => import('@/views/RoutesView.vue'),
          meta: { title: '路线管理' }
        },
        {
          path: 'routes/new',
          name: 'NewRoute',
          component: () => import('@/views/RouteFormView.vue'),
          meta: { title: '新建路线' }
        },
        {
          path: 'routes/:id',
          name: 'RouteDetail',
          component: () => import('@/views/RouteDetailView.vue'),
          meta: { title: '路线详情' }
        },
      ],
    },
  ],
})


router.beforeEach(async (to) => {
  if (to.meta.public) {
    if ((to.name === 'login' || to.name === 'register') && getToken()) {
      return { path: '/dashboard' }
    }
    return true
  }

  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  try {
    const me = await fetchMe()
    if (me.role !== 'ADMIN') {
      return { path: '/login', query: { error: 'user' } }
    }
  } catch {
    return { path: '/login' }
  }

  return true
})

export default router
