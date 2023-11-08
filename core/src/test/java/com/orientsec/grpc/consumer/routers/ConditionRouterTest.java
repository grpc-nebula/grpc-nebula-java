package com.orientsec.grpc.consumer.routers;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.util.MapUtils;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.URL;
import org.junit.Assert;
import org.junit.Test;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for ConditionRouter
 *
 * @author Shawpin Shi
 * @since 2019/5/31
 */
public class ConditionRouterTest {

  @Test
  public void route() throws Exception{
    String serviceName = "com.sxp.TestService";
    String application = "sxp-test-application";
    String project = "sxp-test-project";

    Map<String, String> parameters;




    // 服务端URL和列表
    Map<String, ServiceProvider> serviceProviders = new HashMap<String, ServiceProvider>();

    ServiceProvider provider = new ServiceProvider();
    String providerHost = "192.168.1.1";
    int providerPort = 50001;
    provider.setHost(providerHost);
    provider.setPort(providerPort);

    parameters = new HashMap<>(MapUtils.capacity(5));
    parameters.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.PROVIDERS_CATEGORY);
    parameters.put(GlobalConstants.CommonKey.SIDE, RegistryConstants.PROVIDER_SIDE);
    parameters.put(GlobalConstants.Provider.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.Provider.Key.APPLICATION, application);
    parameters.put(GlobalConstants.CommonKey.PROJECT, project);

    URL providerUrl = new URL(RegistryConstants.GRPC_PROTOCOL, providerHost, providerPort, parameters);
    provider.setUrl(providerUrl);

    serviceProviders.put(providerHost + ":" + providerPort, provider);

    // 客户端URL
    parameters = new HashMap<>(MapUtils.capacity(5));
    parameters.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.CONSUMERS_CATEGORY);
    parameters.put(GlobalConstants.CommonKey.SIDE, RegistryConstants.CONSUMER_SIDE);
    parameters.put(GlobalConstants.Provider.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.Provider.Key.APPLICATION, application);
    parameters.put(GlobalConstants.CommonKey.PROJECT, project);

    String consumerHost = "192.168.2.1";
    int consumerPort = 0;

    URL consumerUrl = new URL(RegistryConstants.GRPC_PROTOCOL, consumerHost, consumerPort, parameters);
    provider.setUrl(providerUrl);

    serviceProviders.put(providerHost + ":" + providerPort, provider);

    Assert.assertTrue(serviceProviders.size() > 0);

    // 黑名单URL
    parameters = new HashMap<>(MapUtils.capacity(5));
    parameters.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.ROUTERS_CATEGORY);
    parameters.put(GlobalConstants.Provider.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.Provider.Key.APPLICATION, application);
    parameters.put(GlobalConstants.CommonKey.PROJECT, project);

    parameters.put("rule", URLEncoder.encode("host=* => ", "UTF-8"));

    URL routerUrl = new URL(RegistryConstants.ROUTER_PROTOCOL, "0.0.0.0", 0, parameters);
    Router router = new ConditionRouter(routerUrl);

    serviceProviders = router.route(serviceProviders, consumerUrl);
    Assert.assertTrue(serviceProviders.size() == 0);

  }
}