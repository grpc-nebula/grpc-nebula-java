package com.orientsec.grpc.provider;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.model.BusinessResult;
import com.orientsec.grpc.common.util.ExceptionUtils;
import com.orientsec.grpc.common.util.GrpcUtils;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.common.util.MapUtils;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.provider.core.ProviderServiceRegistry;
import com.orientsec.grpc.provider.core.ProviderServiceRegistryFactory;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.service.Provider;
import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用不重启的情况下，注册新服务注册或更新已注册服务
 *
 * @author sxp
 * @since 2019/7/9
 */
public class Registry {
  private static Logger logger = LoggerFactory.getLogger(Registry.class);

  /**
   * 注册新服务
   *
   * @param server            服务端对象，即新服务要注册到哪个服务端上面
   * @param serviceDefinition 要注册的新服务的定义，可以从中提取出服务名和方法名
   * @return 返回一个操作结果，封装了操作是否成功的布尔值，以及操作过程中产生的字符串类型的信息
   * @author sxp
   * @since 2019/7/9
   * @since 2019/7/16 modify by sxp 处理操作注册中心，还要操作grpc的中注册的服务数据
   */
  public static BusinessResult registerNewService(Server server, ServerServiceDefinition serviceDefinition) {
    try {
      int port = server.getPort();
      String serviceName = serviceDefinition.getServiceDescriptor().getName();
      String ip = IpUtils.getIP4WithPriority();

      if (StringUtils.isEmpty(serviceName)) {
        return new BusinessResult(false, "服务的名称不能为空");
      }

      Map<String, String> parameters = new HashMap<>(MapUtils.capacity(2));
      parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
      parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
      URL queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, port, parameters);

      Provider provider = new Provider();
      List<URL> urls = provider.lookup(queryUrl);

      if (urls != null && !urls.isEmpty()) {
        return new BusinessResult(false, "在注册中心上已经存在服务名为["
                + serviceName + "]、ip为[" + ip + "]、端口为["
                + port + "]的服务注册信息，不能调用该方法注册新服务。");
      }

      // 获取当前服务端的服务信息
      List<ServerServiceDefinition> services = new ArrayList<>(server.getServices());

      String name;
      for (ServerServiceDefinition item : services) {
        name = item.getServiceDescriptor().getName();
        if (serviceName.equals(name)) {
          return new BusinessResult(false, "服务名为["
                  + serviceName + "]的服务已经存在，不能调用该方法注册新服务。");
        }
      }

      services.add(serviceDefinition);

      // 向服务端对象写入服务信息
      HandlerRegistry registry = server.getRegistry();
      if (registry == null) {
        return new BusinessResult(false, "从服务端对象中无法获取到注册处理器。");
      }
      registry.resetServicesAndMethods(services);

      // 向注册中心写入服务信息
      ProviderServiceRegistry providerRegistry = ProviderServiceRegistryFactory.getRegistry();
      List<Map<String, Object>> params = createParams(serviceDefinition, port);
      providerRegistry.register(params);

      return new BusinessResult(true, "OK");
    } catch (Exception e) {
      logger.error("注册新服务出错", e);
      String message = "注册新服务出错，出错信息堆栈信息为:" + ExceptionUtils.getExceptionStackMsg(e);
      return new BusinessResult(false, message);
    }
  }


  /**
   * 更新已注册的服务
   *
   * @param server            服务端对象，即要更新的服务注册到哪个服务端对象上面
   * @param serviceDefinition 更新后的服务定义，可以从中提取出最新的服务名和方法名
   * @return 返回一个操作结果，封装了操作是否成功的布尔值，以及操作过程中产生的字符串类型的信息
   * @author sxp
   * @since 2019/7/9
   * @since 2019/7/16 modify by sxp 处理操作注册中心，还要操作grpc的中注册的服务数据
   */
  public static BusinessResult updateService(Server server, ServerServiceDefinition serviceDefinition) {
    try {
      int port = server.getPort();
      String serviceName = serviceDefinition.getServiceDescriptor().getName();
      String newMethods = getMethods(serviceDefinition);
      String ip = IpUtils.getIP4WithPriority();

      Map<String, String> parameters = new HashMap<>(MapUtils.capacity(2));
      parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
      parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
      URL queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, port, parameters);

      Provider provider = new Provider();
      List<URL> urls = provider.lookup(queryUrl);

      if (urls == null || urls.isEmpty()) {
        return new BusinessResult(false, "在注册中心上没有查找到服务名为["
                + serviceName + "]、ip为[" + ip + "]、端口为["
                + port + "]的服务注册信息。");
      } else if (urls.size() != 1) {
        return new BusinessResult(false, "在注册中心上服务名为["
                + serviceName + "]、ip为[" + ip + "]、端口为["
                + port + "]的服务注册信息存在[" + urls.size() + "]条。");
      }

      URL providerUrl = urls.get(0);

      // 获取当前服务端的服务信息
      List<ServerServiceDefinition> oldServices = server.getServices();
      List<ServerServiceDefinition> newServices = new ArrayList<>(oldServices.size());

      String name;
      for (ServerServiceDefinition item : oldServices) {
        name = item.getServiceDescriptor().getName();
        if (serviceName.equals(name)) {
          newServices.add(serviceDefinition);
        } else {
          newServices.add(item);
        }
      }

      // 向服务端对象写入服务信息
      HandlerRegistry registry = server.getRegistry();
      if (registry == null) {
        return new BusinessResult(false, "从服务端对象中无法获取到注册处理器。");
      }
      registry.resetServicesAndMethods(newServices);

      // 新的URL
      parameters = new HashMap<>(providerUrl.getParameters());
      parameters.put(GlobalConstants.CommonKey.METHODS, newMethods);
      parameters.put(GlobalConstants.CommonKey.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
      URL newProviderUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, port, parameters);

      // 为了不影响现有服务的调用，先创建新的注册信息，再注销老的注册信息
      provider.registerService(newProviderUrl);
      provider.unRegisterService(providerUrl);

      return new BusinessResult(true, "OK");
    } catch (Exception e) {
      logger.error("更新已注册服务所提供的的方法出错", e);
      String message = "更新已注册服务所提供的的方法出错，出错信息堆栈信息为:" + ExceptionUtils.getExceptionStackMsg(e);
      return new BusinessResult(false, message);
    }
  }


  /**
   * 将服务注册与注销相关的参数封装成一个对象
   *
   * @author sxp
   * @since 2019/7/9
   */
  private static List<Map<String, Object>> createParams(ServerServiceDefinition serviceDefinition, int port) {
    List<ServerServiceDefinition> serviceDfs = new ArrayList<>(1);
    serviceDfs.add(serviceDefinition);

    List<Map<String, Object>> params = new ArrayList<>(serviceDfs.size());

    Map<String, Object> oneService;
    String methods;

    for (ServerServiceDefinition item : serviceDfs) {
      methods = getMethods(item);

      oneService = new HashMap<>();
      oneService.put(GlobalConstants.Provider.Key.INTERFACE, item.getServiceDescriptor().getName());
      oneService.put(GlobalConstants.CommonKey.METHODS, methods);
      oneService.put(GlobalConstants.PROVIDER_SERVICE_PORT, port);

      params.add(oneService);
    }

    return params;
  }

  /**
   * 提取方法名
   *
   * @author sxp
   * @since 2019/7/9
   */
  private static String getMethods(ServerServiceDefinition item) {
    StringBuilder sb = new StringBuilder();
    Collection<MethodDescriptor<?, ?>> methodDesps;
    String methodName;

    methodDesps = item.getServiceDescriptor().getMethods();

    for (MethodDescriptor<?, ?> md : methodDesps) {
      methodName = GrpcUtils.getSimpleMethodName(md.getFullMethodName());
      sb.append(methodName);
      sb.append(",");// 多个方法之间用英文逗号分隔
    }

    sb.deleteCharAt(sb.lastIndexOf(","));

    String methods = sb.toString();
    return methods;
  }

}
