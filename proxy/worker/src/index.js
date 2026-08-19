/**
 * FlowSpark AI 代理层 (Cloudflare Workers)
 *
 * 职责（v2.1 计划书 2.1 节）：
 *  1. API Key 保护：供应商 Key 只存在 Worker 环境变量，App 永远拿不到
 *  2. 供应商路由：/v1/chat/completions → DeepSeek/Groq/OpenAI
 *                   /v1/images/generations → Together AI (Flux) / OpenAI
 *  3. 每日限额：全局 + 按用户维度硬顶
 *  4. 供应商熔断：连续 3 次 5xx 自动切换备用
 *
 * 对外暴露 OpenAI-Compatible 协议，App 端 [OpenAiCompatibleClient] 零感知。
 */

// ============ 供应商路由配置 ============

const PROVIDERS = {
  llm: [
    { name: "deepseek",  base: "https://api.deepseek.com",      model: "deepseek-chat", key: env => env.DEEPSEEK_API_KEY },
    { name: "openai",    base: "https://api.openai.com/v1",     model: "gpt-4.1-mini",   key: env => env.OPENAI_API_KEY },
  ],
  image: [
    { name: "together",  base: "https://api.together.xyz/v1",   model: "black-forest-labs/FLUX.1-schnell", key: env => env.TOGETHER_API_KEY },
    { name: "openai",    base: "https://api.openai.com/v1",     model: "gpt-image-1",    key: env => env.OPENAI_API_KEY },
  ],
};

// 供应商熔断状态（内存态，每个隔离区独立；生产可换 KV 持久化）
const circuitBreakers = new Map(); // key: `${kind}:${name}` -> { failures, openedUntil }

function breakerKey(kind, name) { return `${kind}:${name}`; }

function isOpen(kind, name) {
  const b = circuitBreakers.get(breakerKey(kind, name));
  if (!b) return false;
  if (b.openedUntil && Date.now() < b.openedUntil) return true;
  if (b.openedUntil && Date.now() >= b.openedUntil) { circuitBreakers.delete(breakerKey(kind, name)); return false; }
  return b.failures >= 3;
}

function recordFailure(kind, name) {
  const key = breakerKey(kind, name);
  const b = circuitBreakers.get(key) || { failures: 0, openedUntil: null };
  b.failures += 1;
  if (b.failures >= 3) b.openedUntil = Date.now() + 5 * 60 * 1000; // 5 分钟熔断
  circuitBreakers.set(key, b);
}

function recordSuccess(kind, name) {
  circuitBreakers.delete(breakerKey(kind, name));
}

// ============ 限额控制（KV 持久化） ============

async function checkQuota(env, userId) {
  if (!env.QUOTA_KV) return { allowed: true }; // 未配置 KV 时放行（开发模式）

  const dateKey = new Date().toISOString().slice(0, 10); // YYYY-MM-DD
  const globalKey = `quota:global:${dateKey}`;
  const userKey = `quota:user:${dateKey}:${userId}`;

  const globalLimit = parseInt(env.DAILY_GLOBAL_LIMIT || "1000", 10);
  const userLimit = 50; // 单用户每日 50 次

  const [globalCount, userCount] = await Promise.all([
    env.QUOTA_KV.get(globalKey, { type: "json" }).catch(() => null),
    env.QUOTA_KV.get(userKey, { type: "json" }).catch(() => null),
  ]);

  if ((globalCount || 0) >= globalLimit) {
    return { allowed: false, reason: "今日全局配额已用完" };
  }
  if ((userCount || 0) >= userLimit) {
    return { allowed: false, reason: "今日个人配额已用完" };
  }

  await Promise.all([
    env.QUOTA_KV.put(globalKey, JSON.stringify((globalCount || 0) + 1), { expirationTtl: 86400 }),
    env.QUOTA_KV.put(userKey, JSON.stringify((userCount || 0) + 1), { expirationTtl: 86400 }),
  ]);

  return { allowed: true };
}

// ============ 请求转发 ============

async function forwardRequest(provider, request, env) {
  const upstream = new URL(request.url);
  upstream.host = new URL(provider.base).host;

  // 替换模型名（如果 App 请求的是默认模型名，则映射到供应商实际模型）
  const body = await request.clone().text();
  let parsedBody = null;
  try { parsedBody = JSON.parse(body); } catch (_) { /* 非 JSON 透传 */ }

  if (parsedBody && parsedBody.model) {
    parsedBody.model = provider.model;
  }

  const headers = new Headers(request.headers);
  headers.set("Authorization", `Bearer ${provider.key(env)}`);
  headers.set("Content-Type", "application/json");

  return fetch(new Request(upstream.toString(), {
    method: request.method,
    headers,
    body: parsedBody ? JSON.stringify(parsedBody) : body,
  }));
}

// ============ 主入口 ============

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;

    // CORS（App 原生请求不需要，但 Web 调试方便）
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    };
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    // 简单鉴权：App 持有的代理 Key（可选，推荐开启）
    const proxyKey = env.PROXY_API_KEY;
    if (proxyKey) {
      const auth = request.headers.get("Authorization") || "";
      if (auth !== `Bearer ${proxyKey}`) {
        return new Response(JSON.stringify({
          error: { message: "代理鉴权失败", type: "auth_error" },
        }), { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } });
      }
    }

    // 用户标识（生产应换成签名校验后的用户 ID；demo 用 IP）
    const userId = request.headers.get("CF-Connecting-IP") || "anonymous";

    // 路径路由
    let kind = null;
    if (path.endsWith("/v1/chat/completions")) kind = "llm";
    else if (path.endsWith("/v1/images/generations")) kind = "image";

    if (!kind) {
      return new Response(JSON.stringify({
        error: { message: "未知路径: " + path, type: "not_found" },
      }), { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    // 限额检查
    const quota = await checkQuota(env, userId);
    if (!quota.allowed) {
      return new Response(JSON.stringify({
        error: { message: quota.reason, type: "quota_exceeded", code: "quota_exceeded" },
      }), { status: 429, headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    // 供应商路由 + 熔断
    const providers = PROVIDERS[kind];
    const healthy = providers.filter(p => !isOpen(kind, p.name));
    const candidates = healthy.length > 0 ? healthy : providers; // 全熔断时降级尝试

    for (const provider of candidates) {
      try {
        const upstream = await forwardRequest(provider, request, env);
        if (upstream.status >= 500) {
          recordFailure(kind, provider.name);
          continue; // 尝试下一个供应商
        }
        recordSuccess(kind, provider.name);
        const respHeaders = new Headers(upstream.headers);
        Object.entries(corsHeaders).forEach(([k, v]) => respHeaders.set(k, v));
        return new Response(upstream.body, { status: upstream.status, headers: respHeaders });
      } catch (e) {
        recordFailure(kind, provider.name);
      }
    }

    return new Response(JSON.stringify({
      error: { message: "所有 AI 供应商暂不可用，请稍后重试", type: "all_providers_down" },
    }), { status: 503, headers: { ...corsHeaders, "Content-Type": "application/json" } });
  },
};
