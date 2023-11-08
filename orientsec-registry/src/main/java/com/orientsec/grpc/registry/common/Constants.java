/*
 * Copyright 2018-2019 The Apache Software Foundation
 * Modifications 2019 Orient Securities Co., Ltd.
 * Modifications 2019 BoCloud Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientsec.grpc.registry.common;



import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

/**
 * 常量
 *
 * @author heiden
 * @since 2018/3/15
 */
public class Constants {
  /**
   * 一个服务提供者
   */
  public static final String PROVIDER = "provider";

  /**
   * 一个服务消费者
   */
  public static final String CONSUMER = "consumer";

  /**
   * 一个注册对象
   */
  public static final String REGISTER = "register";

  /**
   * 一个取消注册对象
   */
  public static final String UNREGISTER = "unregister";

  /**
   * 订阅
   */
  public static final String SUBSCRIBE = "subscribe";

  /**
   * 取消订阅
   */
  public static final String UNSUBSCRIBE = "unsubscribe";

  /**
   * 分类
   */
  public static final String CATEGORY_KEY = "category";

  /**
   * 服务提供者集合
   */
  public static final String PROVIDERS_CATEGORY = "providers";

  /**
   * 服务消费者集合
   */
  public static final String CONSUMERS_CATEGORY = "consumers";

  /**
   * 路由规则集合
   */
  public static final String ROUTERS_CATEGORY = "routers";

  /**
   * 配置集合
   */
  public static final String CONFIGURATORS_CATEGORY = "configurators";

  /**
   * 默认分类
   */
  public static final String DEFAULT_CATEGORY = PROVIDERS_CATEGORY;

  /**
   * 启用
   */
  public static final String ENABLED_KEY = "enabled";

  /**
   * 禁用
   */
  public static final String DISABLED_KEY = "disabled";

  /**
   * 验证
   */
  public static final String VALIDATION_KEY = "validation";

  /**
   * 缓存
   */
  public static final String CACHE_KEY = "cache";

  /**
   * 动态
   */
  public static final String DYNAMIC_KEY = "dynamic";

  /**
   * grpc配置文件
   */
  public static final String GRPC_PROPERTIES_KEY = "grpc.properties.file";

  /**
   * grpc属性文件
   */
  public static final String DEFAULT_GRPC_PROPERTIES = "grpc.properties";

  /**
   * 发送标志
   */
  public static final String SENT_KEY = "sent";

  /**
   * 默认发送标志
   */
  public static final boolean DEFAULT_SENT = false;

  /**
   * 注册
   */
  public static final String REGISTRY_PROTOCOL = "registry";

  /**
   * 调用
   */
  public static final String $INVOKE = "$invoke";

  /**
   * 回应
   */
  public static final String $ECHO = "$echo";

  /**
   * 默认文件操作
   */
  public static final int DEFAULT_IO_THREADS = Runtime.getRuntime()
          .availableProcessors() + 1;

  /**
   * 默认代理
   */
  public static final String DEFAULT_PROXY = "javassist";

  /**
   * 默认数据量大小
   */
  public static final int DEFAULT_PAYLOAD = 8 * 1024 * 1024;  // 8M

  /**
   * 默认集群
   */
  public static final String DEFAULT_CLUSTER = "failover";

  /**
   * 默认负载均衡算法
   */
  public static final String DEFAULT_LOADBALANCE = "random";

  /**
   * 默认协议
   */
  public static final String DEFAULT_PROTOCOL = "grpc";

  /**
   * 默认交换对象
   */
  public static final String DEFAULT_EXCHANGER = "header";

  /**
   * 默认transporter对象类型
   */
  public static final String DEFAULT_TRANSPORTER = "netty";

  /**
   * 默认远程服务器对象类型
   */
  public static final String DEFAULT_REMOTING_SERVER = "netty";

  /**
   * 默认远程客户端对象类型
   */
  public static final String DEFAULT_REMOTING_CLIENT = "netty";

  /**
   * 默认远程序列化方式
   */
  public static final String DEFAULT_REMOTING_SERIALIZATION = "hessian2";

  /**
   * 默认HTTP服务器类型
   */
  public static final String DEFAULT_HTTP_SERVER = "servlet";

  /**
   * 默认HTTP客户端类型
   */
  public static final String DEFAULT_HTTP_CLIENT = "jdk";

  /**
   * 默认HTTP序列化类型
   */
  public static final String DEFAULT_HTTP_SERIALIZATION = "json";

  /**
   * 默认字符集
   */
  public static final String DEFAULT_CHARSET = "UTF-8";

  /**
   * 默认权重
   */
  public static final int DEFAULT_WEIGHT = 100;

  /**
   * 默认分支
   */
  public static final int DEFAULT_FORKS = 2;

  /**
   * 默认线程名称
   */
  public static final String DEFAULT_THREAD_NAME = "Grpc";

  /**
   * 默认核心线程数
   */
  public static final int DEFAULT_CORE_THREADS = 0;

  /**
   * 默认线程数
   */
  public static final int DEFAULT_THREADS = 200;

  /**
   * 默认队列数
   */
  public static final int DEFAULT_QUEUES = 0;

  /**
   * 默认存活时间
   */
  public static final int DEFAULT_ALIVE = 60 * 1000;

  /**
   * 默认连接数
   */
  public static final int DEFAULT_CONNECTIONS = 0;

  /**
   * 默认接收数量
   */
  public static final int DEFAULT_ACCEPTS = 0;

  /**
   * 默认空闲超时时间
   */
  public static final int DEFAULT_IDLE_TIMEOUT = 600 * 1000;

  /**
   * 默认心跳时间
   */
  public static final int DEFAULT_HEARTBEAT = 60 * 1000;

  /**
   * 默认超时时间
   */
  public static final int DEFAULT_TIMEOUT = 1000;

  /**
   *
   */
  public static final int DEFAULT_CONNECT_TIMEOUT = 3000;

  /**
   *
   */
  public static final int DEFAULT_RETRIES = 2;

  /**
   * 默认缓冲区大小
   */
  // default buffer size is 8k.
  public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

  /**
   * 最大缓冲区大小
   */
  public static final int MAX_BUFFER_SIZE = 16 * 1024;

  /**
   * 最小缓冲区大小
   */
  public static final int MIN_BUFFER_SIZE = 1 * 1024;

  /**
   * 删除值前缀
   */
  public static final String REMOVE_VALUE_PREFIX = "-";

  /**
   * 隐藏值前缀
   */
  public static final String HIDE_KEY_PREFIX = ".";

  /**
   * 默认值前缀
   */
  public static final String DEFAULT_KEY_PREFIX = "default.";

  /**
   * 默认
   */
  public static final String DEFAULT_KEY = "default";

  /**
   * 负载均衡
   */
  public static final String LOADBALANCE_KEY = "loadbalance";

  /**
   * 路由
   */
  // key for router type, for e.g., "script"/"file",
  // corresponding to ScriptRouterFactory.NAME, FileRouterFactory.NAME
  public static final String ROUTER_KEY = "router";

  /**
   * 集群
   */
  public static final String CLUSTER_KEY = "cluster";

  /**
   * 注册
   */
  public static final String REGISTRY_KEY = "registry";

  /**
   * 监听
   */
  public static final String MONITOR_KEY = "monitor";

  /**
   * 方面
   */
  public static final String SIDE_KEY = "side";

  /**
   * 属于服务提供者
   */
  public static final String PROVIDER_SIDE = "provider";

  /**
   * 属于服务消费者
   */
  public static final String CONSUMER_SIDE = "consumer";

  /**
   * 默认注册
   */
  public static final String DEFAULT_REGISTRY = "grpc";

  /**
   * 备份
   */
  public static final String BACKUP_KEY = "backup";

  /**
   * 目录
   */
  public static final String DIRECTORY_KEY = "directory";

  /**
   * 过期
   */
  public static final String DEPRECATED_KEY = "deprecated";

  /**
   * 任意主机key
   */
  public static final String ANYHOST_KEY = "anyhost";

  /**
   * 任意主机的值
   */
  public static final String ANYHOST_VALUE = "0.0.0.0";

  /**
   * 当前主机名
   */
  public static final String LOCALHOST_KEY = "localhost";

  /**
   * 当前主机的IP地址
   */
  public static final String LOCALHOST_VALUE = "127.0.0.1";

  /**
   * 应用
   */
  public static final String APPLICATION_KEY = "application";

  /**
   * 班底
   */
  public static final String LOCAL_KEY = "local";

  /**
   * 桩
   */
  public static final String STUB_KEY = "stub";

  /**
   * 模拟
   */
  public static final String MOCK_KEY = "mock";

  /**
   * 协议
   */
  public static final String PROTOCOL_KEY = "protocol";

  /**
   * 代理
   */
  public static final String PROXY_KEY = "proxy";

  /**
   * 权重
   */
  public static final String WEIGHT_KEY = "weight";

  /**
   * 分支
   */
  public static final String FORKS_KEY = "forks";

  /**
   * 默认线程池类型
   */
  public static final String DEFAULT_THREADPOOL = "limited";

  /**
   * 默认客户端线程池类型
   */
  public static final String DEFAULT_CLIENT_THREADPOOL = "cached";

  /**
   * 线程池
   */
  public static final String THREADPOOL_KEY = "threadpool";

  /**
   * 线程名称
   */
  public static final String THREAD_NAME_KEY = "threadname";

  /**
   * 文件线程
   */
  public static final String IO_THREADS_KEY = "iothreads";

  /**
   * 核心线程
   */
  public static final String CORE_THREADS_KEY = "corethreads";

  /**
   * 线程
   */
  public static final String THREADS_KEY = "threads";

  /**
   * 队列
   */
  public static final String QUEUES_KEY = "queues";

  /**
   * 存活
   */
  public static final String ALIVE_KEY = "alive";

  /**
   * 执行
   */
  public static final String EXECUTES_KEY = "executes";

  /**
   * 缓冲区
   */
  public static final String BUFFER_KEY = "buffer";

  /**
   * 有效负载
   */
  public static final String PAYLOAD_KEY = "payload";

  /**
   * 引用过滤器
   */
  public static final String REFERENCE_FILTER_KEY = "reference.filter";

  /**
   * 调用者监听
   */
  public static final String INVOKER_LISTENER_KEY = "invoker.listener";

  /**
   * 服务过滤器
   */
  public static final String SERVICE_FILTER_KEY = "service.filter";

  /**
   * 导出监听器
   */
  public static final String EXPORTER_LISTENER_KEY = "exporter.listener";

  /**
   * 访问日志
   */
  public static final String ACCESS_LOG_KEY = "accesslog";

  /**
   * 活动的
   */
  public static final String ACTIVES_KEY = "actives";

  /**
   * 连接数
   */
  public static final String CONNECTIONS_KEY = "connections";

  /**
   * 接受数
   */
  public static final String ACCEPTS_KEY = "accepts";

  /**
   * 空闲超时
   */
  public static final String IDLE_TIMEOUT_KEY = "idle.timeout";

  /**
   * 心跳
   */
  public static final String HEARTBEAT_KEY = "heartbeat";

  /**
   * 心跳超时时间
   */
  public static final String HEARTBEAT_TIMEOUT_KEY = "heartbeat.timeout";

  /**
   * 连接超时时间
   */
  public static final String CONNECT_TIMEOUT_KEY = "connect.timeout";

  /**
   * 超时时间
   */
  public static final String TIMEOUT_KEY = "timeout";

  /**
   * 重试
   */
  public static final String RETRIES_KEY = "retries";

  /**
   * 提示
   */
  public static final String PROMPT_KEY = "prompt";

  /**
   * 默认提示符
   */
  public static final String DEFAULT_PROMPT = "grpc>";

  /**
   * 编码
   */
  public static final String CODEC_KEY = "codec";

  /**
   * 序列化
   */
  public static final String SERIALIZATION_KEY = "serialization";

  /**
   * 交换者
   */
  public static final String EXCHANGER_KEY = "exchanger";

  /**
   * 传输者
   */
  public static final String TRANSPORTER_KEY = "transporter";

  /**
   * 服务器
   */
  public static final String SERVER_KEY = "server";

  /**
   * 客户端
   */
  public static final String CLIENT_KEY = "client";

  /**
   * 唯一标示符
   */
  public static final String ID_KEY = "id";

  /**
   * 异步
   */
  public static final String ASYNC_KEY = "async";

  /**
   * 返回
   */
  public static final String RETURN_KEY = "return";

  /**
   * 令牌
   */
  public static final String TOKEN_KEY = "token";

  /**
   * 方法
   */
  public static final String METHOD_KEY = "method";

  /**
   * 方法集合
   */
  public static final String METHODS_KEY = "methods";

  /**
   * 字符集
   */
  public static final String CHARSET_KEY = "charset";

  /**
   * 重新连接
   */
  public static final String RECONNECT_KEY = "reconnect";

  /**
   * 发送重连
   */
  public static final String SEND_RECONNECT_KEY = "send.reconnect";

  /**
   * 默认重连周期
   */
  public static final int DEFAULT_RECONNECT_PERIOD = 2000;

  /**
   * 关闭超时时间key
   */
  public static final String SHUTDOWN_TIMEOUT_KEY = "shutdown.timeout";

  /**
   * 默认关机超时时间
   */
  public static final int DEFAULT_SHUTDOWN_TIMEOUT = 1000 * 60 * 15;

  /**
   * 进程ID
   */
  public static final String PID_KEY = "pid";

  /**
   * 时间戳
   */
  public static final String TIMESTAMP_KEY = "timestamp";

  /**
   * 热身key
   */
  public static final String WARMUP_KEY = "warmup";

  /**
   * 默认热身时间
   */
  public static final int DEFAULT_WARMUP = 10 * 60 * 1000;

  /**
   * 检验
   */
  public static final String CHECK_KEY = "check";

  /**
   * 注册
   */
  public static final String REGISTER_KEY = "register";

  /**
   * 订阅
   */
  public static final String SUBSCRIBE_KEY = "subscribe";

  /**
   * 分组
   */
  public static final String GROUP_KEY = "group";

  /**
   * 路径
   */
  public static final String PATH_KEY = "path";

  /**
   * 接口
   */
  public static final String INTERFACE_KEY = "interface";

  /**
   * 通用的
   */
  public static final String GENERIC_KEY = "generic";

  /**
   * 文件
   */
  public static final String FILE_KEY = "file";

  /**
   * 等待
   */
  public static final String WAIT_KEY = "wait";

  /**
   * 分类器
   */
  public static final String CLASSIFIER_KEY = "classifier";

  /**
   * 版本
   */
  public static final String VERSION_KEY = "version";

  /**
   * 历史版本
   */
  public static final String REVISION_KEY = "revision";

  /**
   * grpc版本的key值
   */
  public static final String GRPC_VERSION_KEY = "grpc";

  /**
   * hessian版本
   */
  public static final String HESSIAN_VERSION_KEY = "hessian.version";

  /**
   * 调度
   */
  public static final String DISPATCHER_KEY = "dispatcher";

  /**
   * 通道处理这
   */
  public static final String CHANNEL_HANDLER_KEY = "channel.handler";

  /**
   * 默认
   */
  public static final String DEFAULT_CHANNEL_HANDLER = "default";

  /**
   * 任意值的通配符
   */
  public static final String ANY_VALUE = "*";

  /**
   * 逗号
   */
  public static final String COMMA_SEPARATOR = ",";

  /**
   * 逗号分隔正则匹配模式
   */
  public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

  /**
   * 路径分隔符
   */
  public static final String PATH_SEPARATOR = "/";

  /**
   * 注册分隔符
   */
  public static final String REGISTRY_SEPARATOR = "|";

  /**
   * 注册则正匹配模式
   */
  public static final Pattern REGISTRY_SPLIT_PATTERN = Pattern.compile("\\s*[|;]+\\s*");

  /**
   * 分号
   */
  public static final String SEMICOLON_SEPARATOR = ";";

  /**
   * 分号正则匹配模式
   */
  public static final Pattern SEMICOLON_SPLIT_PATTERN = Pattern.compile("\\s*[;]+\\s*");

  /**
   * 连接队列容量
   */
  public static final String CONNECT_QUEUE_CAPACITY = "connect.queue.capacity";

  /**
   * 连接队列发出警告时的大小key
   */
  public static final String CONNECT_QUEUE_WARNING_SIZE = "connect.queue.warning.size";

  /**
   *连接队列发出警告时的大小value
   */
  public static final int DEFAULT_CONNECT_QUEUE_WARNING_SIZE = 1000;

  /**
   * 只读通道
   */
  public static final String CHANNEL_ATTRIBUTE_READONLY_KEY = "channel.readonly";

  /**
   * 只读通道已发送
   */
  public static final String CHANNEL_READONLYEVENT_SENT_KEY = "channel.readonly.sent";

  /**
   * 只读通道发送
   */
  public static final String CHANNEL_SEND_READONLYEVENT_KEY = "channel.readonly.send";

  /**
   * 计数
   */
  public static final String COUNT_PROTOCOL = "count";

  /**
   * 跟踪
   */
  public static final String TRACE_PROTOCOL = "trace";

  /**
   * 空
   */
  public static final String EMPTY_PROTOCOL = "empty";

  /**
   * 管理协议
   */
  public static final String ADMIN_PROTOCOL = "admin";

  /**
   * 提供者协议
   */
  public static final String PROVIDER_PROTOCOL = "provider";

  /**
   * 消费者协议
   */
  public static final String CONSUMER_PROTOCOL = "consumer";

  /**
   * 路由协议
   */
  public static final String ROUTE_PROTOCOL = "route";

  public static final String SCRIPT_PROTOCOL = "script";

  public static final String CONDITION_PROTOCOL = "condition";

  public static final String MOCK_PROTOCOL = "mock";

  public static final String RETURN_PREFIX = "return ";

  public static final String THROW_PREFIX = "throw";

  public static final String FAIL_PREFIX = "fail:";

  public static final String FORCE_PREFIX = "force:";

  /**
   * 强制
   */
  public static final String FORCE_KEY = "force";

  public static final String MERGER_KEY = "merger";

  /**
   * 集群时是否排除非available的invoker.
   */
  public static final String CLUSTER_AVAILABLE_CHECK_KEY = "cluster.availablecheck";


  public static final boolean DEFAULT_CLUSTER_AVAILABLE_CHECK = true;

  /**
   * 集群时是否启用sticky策略.
   */
  public static final String CLUSTER_STICKY_KEY = "sticky";

  /**
   * sticky默认值.
   */
  public static final boolean DEFAULT_CLUSTER_STICKY = false;

  /**
   * 创建client时，是否先要建立连接.
   */
  public static final String LAZY_CONNECT_KEY = "lazy";

  /**
   * lazy连接的初始状态是连接状态还是非连接状态.
   */
  public static final String LAZY_CONNECT_INITIAL_STATE_KEY = "connect.lazy.initial.state";

  /**
   * lazy连接的初始状态默认是连接状态.
   */
  public static final boolean DEFAULT_LAZY_CONNECT_INITIAL_STATE = true;

  /**
   * 注册中心是否同步存储文件，默认异步.
   */
  public static final String REGISTRY_FILESAVE_SYNC_KEY = "save.file";

  /**
   * 注册中心失败事件重试事件.
   */
  public static final String REGISTRY_RETRY_PERIOD_KEY = "retry.period";

  /**
   * 重试周期.
   */
  public static final int DEFAULT_REGISTRY_RETRY_PERIOD = 5 * 1000;

  /**
   * 注册中心自动重连时间.
   */
  public static final String REGISTRY_RECONNECT_PERIOD_KEY = "reconnect.period";

  public static final int DEFAULT_REGISTRY_RECONNECT_PERIOD = 3 * 1000;

  public static final String SESSION_TIMEOUT_KEY = "session";

  public static final int DEFAULT_SESSION_TIMEOUT = 60 * 1000;

  /**
   * 注册中心导出URL参数的KEY.
   */
  public static final String EXPORT_KEY = "export";

  /**
   * 注册中心引用URL参数的KEY.
   */
  public static final String REFER_KEY = "refer";

  /**
   * callback inst id.
   */
  public static final String CALLBACK_SERVICE_KEY = "callback.service.instid";

  /**
   * 每个客户端同一个接口 callback服务实例的限制.
   */
  public static final String CALLBACK_INSTANCES_LIMIT_KEY = "callbacks";

  /**
   * 每个客户端同一个接口 callback服务实例的限制.
   */
  public static final int DEFAULT_CALLBACK_INSTANCES = 1;

  public static final String CALLBACK_SERVICE_PROXY_KEY = "callback.service.proxy";

  public static final String IS_CALLBACK_SERVICE = "is_callback_service";

  /**
   * channel中callback的invokers.
   */
  public static final String CHANNEL_CALLBACK_KEY = "channel.callback.invokers.key";

  /**
   * 默认值毫秒，避免重新计算.
   */
  public static final int DEFAULT_SERVER_SHUTDOWN_TIMEOUT = 10000;

  public static final String ON_CONNECT_KEY = "onconnect";

  public static final String ON_DISCONNECT_KEY = "ondisconnect";

  public static final String ON_INVOKE_METHOD_KEY = "oninvoke.method";

  public static final String ON_RETURN_METHOD_KEY = "onreturn.method";

  public static final String ON_THROW_METHOD_KEY = "onthrow.method";

  public static final String ON_INVOKE_INSTANCE_KEY = "oninvoke.instance";

  public static final String ON_RETURN_INSTANCE_KEY = "onreturn.instance";

  public static final String ON_THROW_INSTANCE_KEY = "onthrow.instance";

  /**
   * 覆盖协议
   */
  public static final String OVERRIDE_PROTOCOL = "override";

  /**
   * 优先级
   */
  public static final String PRIORITY_KEY = "priority";

  /**
   * 规则
   */
  public static final String RULE_KEY = "rule";

  /**
   * 类型
   */
  public static final String TYPE_KEY = "type";

  /**
   * 运行时
   */
  public static final String RUNTIME_KEY = "runtime";

  // when ROUTER_KEY's value is set to ROUTER_TYPE_CLEAR,
  // RegistryDirectory will clean all current routers
  public static final String ROUTER_TYPE_CLEAR = "clean";

  public static final String DEFAULT_SCRIPT_TYPE_KEY = "javascript";

  public static final boolean DEFAULT_STUB_EVENT = false;

  /**
   * invocation attachment属性中如果有此值，则选择mock invoker
   */
  public static final String INVOCATION_NEED_MOCK = "invocation.need.mock";

  public static final String LOCAL_PROTOCOL = "injvm";

  public static final String AUTO_ATTACH_INVOCATIONID_KEY = "invocationid.autoattach";

  public static final String SCOPE_KEY = "scope";

  public static final String SCOPE_LOCAL = "local";

  public static final String SCOPE_REMOTE = "remote";

  public static final String SCOPE_NONE = "none";

  public static final String RELIABLE_PROTOCOL = "napoli";

  public static final String TPS_LIMIT_RATE_KEY = "tps";

  public static final String TPS_LIMIT_INTERVAL_KEY = "tps.interval";

  public static final long DEFAULT_TPS_LIMIT_INTERVAL = 60 * 1000;

  public static final String DECODE_IN_IO_THREAD_KEY = "decode.in.io";

  public static final boolean DEFAULT_DECODE_IN_IO_THREAD = true;

  public static final String INPUT_KEY = "input";

  public static final String OUTPUT_KEY = "output";

  public static final String EXECUTOR_SERVICE_COMPONENT_KEY = ExecutorService.class.getName();

  public static final String GENERIC_SERIALIZATION_NATIVE_JAVA = "nativejava";

  public static final String GENERIC_SERIALIZATION_DEFAULT = "true";

  public static final String GENERIC_SERIALIZATION_BEAN = "bean";

  /**
   * 默认zk端口
   */
  public static final int DEFAULT_ZOOKEEPER_PORT = 2181;

  /**
   * 参数路由，用户自定义提示语
   */
  public static final String MESSAGE = "message";

}
