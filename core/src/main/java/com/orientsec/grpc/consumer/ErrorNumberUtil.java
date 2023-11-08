package com.orientsec.grpc.consumer;

import com.orientsec.grpc.common.collect.ConcurrentHashSet;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.PropertiesUtils;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.internal.ProvidersListener;
import com.orientsec.grpc.consumer.internal.ZookeeperNameResolver;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 容错机制
 * <p>
 * 功能：连续多次请求出错，自动切换到提供相同服务的新服务器 <br>
 * 备注：将代码从FailoverUtils中独立出来，便于阅读
 * <p/>
 *
 * @author sxp
 * @since 2019/9/2
 */
public class ErrorNumberUtil {
  private static final Logger logger = LoggerFactory.getLogger(ErrorNumberUtil.class);

  private static final String CONSUMERID_PROVIDERID_SEPARATOR = FailoverUtils.CONSUMERID_PROVIDERID_SEPARATOR;

  private static Properties properties = SystemConfig.getProperties();

  /**
   * 连续多少次请求出错，自动切换到提供相同服务的新服务器
   */
  private static int switchoverThreshold = initThreshold();

  /** 服务恢复时间 */
  private static int recoveryMilliseconds = initRecoveryMilliseconds();

  private volatile static ScheduledExecutorService timerService = null;

  private static int initThreshold() {
    String key = GlobalConstants.Consumer.Key.SWITCHOVER_THRESHOLD;
    int defaultValue = 5;

    int threshold = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (threshold <= 0) {
      threshold = defaultValue;
    }

    logger.info(key + " = " + threshold);

    return threshold;
  }

  public static int initRecoveryMilliseconds() {
    String key = GlobalConstants.Consumer.Key.RECOVERY_MILLISECONDS;
    int defaultValue = 600000;// 即600秒，即10分钟

    int recoveryTime = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (recoveryTime < 0) {
      recoveryTime = defaultValue;
    }

    logger.info(key + " = " + recoveryTime);

    return recoveryTime;
  }

  /**
   * 调用失败的【客户端对应服务提供者列表】
   * <p>
   * key值为：consumerId  <br>
   * value值为: 服务提供者IP:port的列表  <br>
   * 其中consumerId指的是客户端在zk上注册的URL的字符串形式，IP:port指的是服务提供者的IP和端口
   * <p/>
   */
  private volatile static ConcurrentHashMap<String, ConcurrentHashSet<String>> failingProviders = new ConcurrentHashMap<>();

  /**
   * 各个【客户端对应服务提供者】最后一次服务调用失败时间
   * <p>
   * key值为：consumerId@IP:port  <br>
   * value值为: 最后一次调用失败时间，以当时的毫秒时间戳记录   <br>
   * 其中consumerId指的是客户端在zk上注册的URL的字符串形式，@是分隔符，IP:port指的是服务提供者的IP和端口
   * <p/>
   */
  private volatile static Map<String, Long> lastFailingTime = new HashMap<>();

  /**
   * 各个【客户端对应服务提供者】服务调用失败次数
   * <p>
   * key值为：consumerId@IP:port  <br>
   * value值为: 失败次数   <br>
   * 其中consumerId指的是客户端在zk上注册的URL的字符串形式，@是分隔符，IP:port指的是服务提供者的IP和端口
   * <p/>
   */
  private volatile static ConcurrentHashMap<String, AtomicInteger> requestFailures = new ConcurrentHashMap<>();


  /**
   * 记录调用情况
   *
   * @author sxp
   * @since 2018-6-21
   * @since 2019/12/10 modify by wlh 增加调用成功/失败标识，根据标识判断执行服务操作逻辑
   */
  public static <ReqT, RespT> void recordInvokeInfo(ClientCall<ReqT, RespT> call, Channel channel, String method, boolean success, Exception e) {
    if (channel == null) {
      return;
    }

    NameResolver nameResolver = channel.getNameResolver();
    if (nameResolver == null) {
      return;
    }

    String consumerId = nameResolver.getSubscribeId();
    if (StringUtils.isEmpty(consumerId)) {
      return;
    }

    String providerId = FailoverUtils.getProviderId(channel, e);
    if (StringUtils.isEmpty(providerId)) {
      return;
    }

    if (!success) {
      logger.info("Bad provider is: " + providerId);
    }

    Object argument = FailoverUtils.getArgument(nameResolver);

    String key = consumerId + CONSUMERID_PROVIDERID_SEPARATOR + providerId;

    long currentTimestamp = System.currentTimeMillis();
    long lastTimestamp;  // 最后一次服务调用失败时间

    if (lastFailingTime.containsKey(key)) {
      lastTimestamp = lastFailingTime.get(key);
    } else {
      lastTimestamp = currentTimestamp;
    }
    lastFailingTime.put(key, currentTimestamp);

    // 更新客户端对应服务提供者列表
    updateFailingProviders(consumerId, providerId, success);

    if (success) {
      // 重置失败次数
      resetFailTimes(nameResolver, consumerId, providerId, argument, method);
    } else {
      // 更新失败次数
      updateFailTimes(channel, nameResolver, consumerId, providerId, lastTimestamp, currentTimestamp, argument, method);
    }
  }

  /**
   * 更新客户端对应服务提供者列表
   *
   * @author sxp
   * @since 2018-6-25
   * @since 2019/12/10 modify by wlh 增加请求成功判断，成功则将服务从失败列表中移除。
   */
  private static void updateFailingProviders(String consumerId, String providerId, boolean success) {
    ConcurrentHashSet<String> providers;

    if (failingProviders.containsKey(consumerId)) {
      providers = failingProviders.get(consumerId);
    } else {
      providers = new ConcurrentHashSet<>();
      ConcurrentHashSet<String> oldValue = failingProviders.putIfAbsent(consumerId, providers);
      if (oldValue != null) {
        providers = oldValue;
      }
    }

    try {
      if (success && providers.contains(providerId)) {
        // 请求成功，将服务从失败列表中移除
        providers.remove(providerId);
      } else if (!success && !providers.contains(providerId)) {
        providers.add(providerId);
      }
    } catch (Exception e) {
      logger.info("更新客户端对应服务提供者列表出错", e);
    }
  }

  /**
   * 服务调用失败次数
   *
   * @author sxp
   * @since 2018-6-25
   */
  private static void updateFailTimes(Channel channel, NameResolver nameResolver, String consumerId, String providerId,
                                      long lastTimestamp, long currentTimestamp, Object argument, String method) {
    AtomicInteger failTimes;// 失败次数

    String key = consumerId + CONSUMERID_PROVIDERID_SEPARATOR + providerId;

    if (!requestFailures.containsKey(key)) {
      failTimes = new AtomicInteger(0);
      AtomicInteger oldValue = requestFailures.putIfAbsent(key, failTimes);
      if (oldValue != null) {
        failTimes = oldValue;
      }
    } else {
      failTimes = requestFailures.get(key);
    }

    failTimes.incrementAndGet();

    int consumerProvidersAmount;// 客户端服务列表中服务提供者的数量
    boolean isZkProviderListEmpty = isZkProviderListEmpty(nameResolver);// 注册中心上服务提供者列表是否为空

    if (failTimes.get() >= switchoverThreshold) {
      removeCurrentProvider(nameResolver, providerId, method);

      consumerProvidersAmount = getConsumerProvidersAmount(nameResolver);

      if (consumerProvidersAmount > 0) {
        try {
          logger.info("重选服务提供者...");
          nameResolver.resolveServerInfo(argument, method);
        } catch (Throwable t) {
          logger.error("重选服务提供者出错", t);
        }
      }

      failTimes.set(0);// 重置请求出错次数
    }

    consumerProvidersAmount = getConsumerProvidersAmount(nameResolver);// 重新获取

    if (consumerProvidersAmount == 0 && !isZkProviderListEmpty
            && (nameResolver instanceof ZookeeperNameResolver)) {
      ZookeeperNameResolver zkResolver = (ZookeeperNameResolver) nameResolver;

      String serviceName = zkResolver.getServiceName();
      Object lock = zkResolver.getLock();

      synchronized (lock) {// 这里相当于模拟服务列表发生变化，需要加锁
        logger.info("重新查询一遍服务提供者，将注册中心上的服务列表写入当前消费者的服务列表...");
        zkResolver.getAllByName(serviceName);

        try {
          logger.info("重选服务提供者......");
          nameResolver.resolveServerInfo(argument, method);
        } catch (Throwable t) {
          logger.error("重选服务提供者出错", t);
        }
      }
    }
  }

  /**
   * 将当前出错的服务器从备选列表中去除
   *
   * @author sxp
   * @since 2018-6-21
   * @since 2019/12/11 modify by wlh 10分钟（时间可配）后，将服务重新放回至服务提供列表
   */
  private static void removeCurrentProvider(NameResolver nameResolver, String providerId, String method) {
    Map<String, ServiceProvider> providersForLoadBalance = nameResolver.getProvidersForLoadBalance();
    if (providersForLoadBalance == null || providersForLoadBalance.size() == 0) {
      logger.info("客户端的备选列表为空", providerId);
      return;
    }

    if (providersForLoadBalance.containsKey(providerId)) {
      logger.error("FATAL ERROR : 服务器节点{}连续调用出错{}次，从客户端备选服务器列表中删除", providerId, switchoverThreshold);
      providersForLoadBalance.remove(providerId);
      nameResolver.reCalculateProvidersCountAfterLoadBalance(method);

      if (timerService == null) {
        timerService = SharedResourceHolder.get(GrpcUtil.TIMER_SERVICE);
      }
      timerService.schedule(new RecoveryServerRunnable(nameResolver, providerId, method), recoveryMilliseconds, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * 获取客户端服务列表中服务提供者的数量
   *
   * @author sxp
   * @since 2018-7-7
   */
  private static int getConsumerProvidersAmount(NameResolver nameResolver) {
    Map<String, ServiceProvider> providersForLoadBalance = nameResolver.getProvidersForLoadBalance();
    if (providersForLoadBalance == null) {
      return 0;
    }
    return providersForLoadBalance.size();
  }

  /**
   * 注册中心上该消费者的服务提供者列表是否为空
   *
   * @author sxp
   * @since 2018-7-7
   */
  private static boolean isZkProviderListEmpty(NameResolver nameResolver) {
    ProvidersListener listener = nameResolver.getProvidersListener();
    if (listener == null) {
      return true;// 为空
    }

    return listener.isProviderListEmpty();
  }


  /**
   * 删除与当前客户端相关的数据(服务调用出错次数、时间、当前客户端对应的服务提供者列表)
   *
   * @author sxp
   * @since 2018-6-25
   */
  static void removeDateByConsumerId(String consumerId) {
    if (!failingProviders.containsKey(consumerId)) {
      return;// consumerId对应的客户端没有出现过调用出错的情况
    }

    ConcurrentHashSet<String> providerIds = failingProviders.get(consumerId);
    String key;

    for (String providerId : providerIds) {
      key = consumerId + CONSUMERID_PROVIDERID_SEPARATOR + providerId;
      lastFailingTime.remove(key);// 服务最后一次调用出错时间
      requestFailures.remove(key);// 服务调用出错次数
    }

    failingProviders.remove(consumerId);// 服务提供者列表
  }

  /**
   * 将服务重新添加到服务提供者列表中
   *
   * @author wlh
   * @since 2019/12/11
   */
  public static void addCurrentProvider(NameResolver nameResolver, String providerId, String method) {
    Map<String, ServiceProvider> providersForLoadBalance = nameResolver.getProvidersForLoadBalance();

    if (providersForLoadBalance != null && !providersForLoadBalance.containsKey(providerId)) {
      Map<String, ServiceProvider> allProviders = nameResolver.getAllProviders();
      ServiceProvider serviceProvider = allProviders.get(providerId);

      if (serviceProvider != null) {
        logger.info("服务器节点{}被重新添加到客户端备选服务器列表中", providerId);
        providersForLoadBalance.put(providerId, serviceProvider);
        nameResolver.reCalculateProvidersCountAfterLoadBalance(method);
      }
    }
  }

  /**
   * 重置服务失败次数为0
   *
   * @author wlh
   * @since 2019/12/11
   */
  public static void resetFailTimes(NameResolver nameResolver, String consumerId, String providerId, Object argument, String method) {
    String key = consumerId + CONSUMERID_PROVIDERID_SEPARATOR + providerId;
    if (requestFailures.containsKey(key)) {
      requestFailures.get(key).set(0);
    }
  }

  public static class RecoveryServerRunnable implements Runnable {

    private NameResolver nameResolver;
    private String providerId;
    private String method;

    public RecoveryServerRunnable(NameResolver nameResolver, String providerId, String method) {
      this.nameResolver = nameResolver;
      this.providerId = providerId;
      this.method = method;
    }

    @Override
    public void run() {
      addCurrentProvider(nameResolver, providerId, method);
    }
  }
}
