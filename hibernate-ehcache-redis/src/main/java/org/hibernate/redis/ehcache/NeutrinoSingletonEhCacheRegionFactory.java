package org.hibernate.redis.ehcache;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.ehcache.internal.util.HibernateEhcacheUtils;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.redis.ehcache.exceptions.SystemException;
import org.hibernate.redis.ehcache.redis.RedisClientMode;
import org.jboss.logging.Logger;
import org.redisson.Redisson;
import org.redisson.api.BatchOptions;
import org.redisson.api.BatchOptions.ExecutionMode;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.LocalCachedMapOptions.ReconnectionStrategy;
import org.redisson.api.LocalCachedMapOptions.SyncStrategy;
import org.redisson.codec.FstCodec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.redisson.connection.balancer.LoadBalancer;
import org.redisson.connection.balancer.RoundRobinLoadBalancer;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;

public class NeutrinoSingletonEhCacheRegionFactory extends AbstractEhcacheRegionFactory implements RegionFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(EhCacheMessageLogger.class,
			NeutrinoSingletonEhCacheRegionFactory.class.getName());

	private static final String HIBERNATE_CACHE_REDIS_CONFIGURATION_RESOURCE_NAME = "hibernate.cache.redis.cfg";
	private static final String HIBERNATE_CACHE_DEFAULT_REDIS_CONFIGURATION_FILE = "config-properties/redis-ehcache-config.properties";
	private static final String REDIS_CACHE_MODE = "hib.redis.cache.mode";
	
	private static final String REDIS_SERVER_IDLE_CONNECTION_TIMEOUT = "hib.redis.server.idle.connection.timeout";
	private static final String REDIS_SERVER_PING_INTERVAL = "hib.redis.server.ping.interval";
	private static final String REDIS_SERVER_PING_TIMEOUT = "hib.redis.server.ping.timeout";
	private static final String REDIS_SERVER_CONNECT_TIMEOUT = "hib.redis.server.connect.timeout";
	private static final String REDIS_SERVER_TIMEOUT = "hib.redis.server.timeout";
	private static final String REDIS_SERVER_RETRY_ATTEMPTS = "hib.redis.server.retry.attempts";
	private static final String REDIS_SERVER_RECONNECTION_TIMEOUT = "hib.redis.server.reconnection.timeout";
	private static final String REDIS_SERVER_FAILED_ATTEMPTS = "hib.redis.server.failed.attempts";
	private static final String REDIS_SERVER_SUBSCRIPTION_PER_CONNECTION = "hib.redis.server.subscription.per.connection";
	private static final String REDIS_SERVER_SUBS_CONN_MIN_IDLE_SIZE = "hib.redis.server.subscription.connection.min.idle.size";
	private static final String REDIS_SERVER_SUBS_CONN_POOL_SIZE = "hib.redis.server.subscription.connection.pool.size";
	private static final String REDIS_SERVER_DATABASE_ID = "hib.redis.server.database.id";
	private static final String REDIS_SERVER_PASSWORD_KEY = "hib.redis.server.password";

	private static final String REDIS_SINGLE_SERVER_ADDRESS = "hib.redis.server.address";
	private static final String REDIS_SINGLE_SERVER_CLIENT_NAME = "hib.redis.server.client.name";
	private static final String REDIS_SINGLE_SERVER_CONNECTION_MIN_IDLE_SIZE = "hib.redis.server.connection.min.idle.size";
	private static final String REDIS_SINGLE_SERVER_CONNECTION_POOL_SIZE = "hib.redis.server.connection.pool.size";

	private static final String REDIS_SENTINEL_SERVER_CLIENT_NAME = "hib.redis.sentinel.client.master.name";
	private static final String REDIS_SENTINEL_SLAVE_CONNECTION_MIN_IDLE_SIZE = "hib.redis.sentinel.slave.connection.min.idle.size";
	private static final String REDIS_SENTINEL_SLAVE_CONNECTION_POOL_SIZE = "hib.redis.sentinel.slave.connection.pool.size";
	private static final String REDIS_SENTINEL_MASTER_CONNECTION_MIN_IDLE_SIZE = "hib.redis.sentinel.master.connection.min.idle.size";
	private static final String REDIS_SENTINEL_MASTER_CONNECTION_POOL_SIZE = "hib.redis.sentinel.master.connection.pool.size";
	private static final String REDIS_SENTINEL_SERVER_MASTER_NAME = "hib.redis.sentinel.master.name";
	private static final String REDIS_SENTINEL_SERVER_ADDRESS_A = "hib.redis.sentinel.addressA";
	private static final String REDIS_SENTINEL_SERVER_ADDRESS_B = "hib.redis.sentinel.addressB";
	private static final String REDIS_SENTINEL_SERVER_ADDRESS_C = "hib.redis.sentinel.addressC";
	private static final String REDIS_SENTINEL_SERVER_ADDRESS_D = "hib.redis.sentinel.addressD";

	private static final String REDIS_SERVER_BATCH_RESPONSE_TIMEOUT = "hib.redis.server.batch.response.timeout";
	private static final String REDIS_SERVER_BATCH_RETRY_ATTEMPTS = "hib.redis.server.batch.retry.attempts";
	private static final String REDIS_SERVER_BATCH_RETRY_INTERVAL = "hib.redis.server.batch.retry.interval";

	private static final AtomicInteger REFERENCE_COUNT = new AtomicInteger();

	/**
	 * Constructs a NeutrinoSingletonEhCacheRegionFactory
	 */
	public NeutrinoSingletonEhCacheRegionFactory() {
	}

	/**
	 * Constructs a NeutrinoSingletonEhCacheRegionFactory
	 *
	 * @param prop
	 *            Not used
	 */
	public NeutrinoSingletonEhCacheRegionFactory(Properties prop) {
		super();
	}

	@Override
	public void start(SessionFactoryOptions settings, Properties properties) throws CacheException {
		this.settings = settings;
		try {
			String configurationResourceName = null;
			String redisConfigurationResourceName = null;

			if (properties != null) {
				configurationResourceName = (String) properties.get(NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME);
				redisConfigurationResourceName = (String) properties
						.get(HIBERNATE_CACHE_REDIS_CONFIGURATION_RESOURCE_NAME);
				if (redisConfigurationResourceName == null || redisConfigurationResourceName.length() <= 0) {
					redisConfigurationResourceName = HIBERNATE_CACHE_DEFAULT_REDIS_CONFIGURATION_FILE;
				}
			} else {
				throw new SystemException("System Properties Not Found");
			}
			if (configurationResourceName == null || configurationResourceName.length() == 0) {
				manager = CacheManager.create();
				REFERENCE_COUNT.incrementAndGet();
			} else {
				URL url = createURL(configurationResourceName);
				final Configuration configuration = HibernateEhcacheUtils.loadAndCorrectConfiguration(url);
				manager = CacheManager.create(configuration);
				REFERENCE_COUNT.incrementAndGet();
			}
			if (redisson == null && redisConfigurationResourceName != null
					&& redisConfigurationResourceName.length() > 0) {
				URL url = createURL(redisConfigurationResourceName);
				Properties prop = loadPropertiesFromURL(url);
				RedisClientMode redisClientMode = getRedisClientMode(prop);
				if(!redisClientMode.isRedisEnabled()) {
					throw new SystemException("Wrong Redis Client Mode. Allowed values for property '"
							+ REDIS_CACHE_MODE + "' are either '" + RedisClientMode.REDIS_SINGLE + "' or '"
							+ RedisClientMode.REDIS_SENTINEL + "'");
				}
				if (redisClientMode.equals(RedisClientMode.SENTINEL)) {
					initRedissonSentinelClient(prop);
				} else {
					initRedissonSingleServerClient(prop);
				}

			}
			mbeanRegistrationHelper.registerMBean(manager, properties);
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public void stop() {
		try {
			if (manager != null) {
				if (REFERENCE_COUNT.decrementAndGet() == 0) {
					manager.shutdown();
				}
				manager = null;
			}
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
	}
	
	private RedisClientMode getRedisClientMode(Properties prop) {
		String redisMode = getProperty(prop, REDIS_CACHE_MODE);
		if (redisMode != null) {
			if (redisMode.equalsIgnoreCase(RedisClientMode.REDIS_SENTINEL)) {
				return RedisClientMode.SENTINEL;
			} else if (redisMode.equalsIgnoreCase(RedisClientMode.REDIS_SINGLE)) {
				return RedisClientMode.SINGLE_SERVER;
			} else {
				LOG.error("Invalid property found for " + REDIS_CACHE_MODE);
				return RedisClientMode.LOCAL;
			}
		} else {
			return RedisClientMode.SINGLE_SERVER;
		}
	}

	private URL createURL(String configurationResourceName) {
		URL url = null;
		try {
			url = new URL(configurationResourceName);
		} catch (MalformedURLException e) {
			if (!configurationResourceName.startsWith("/")) {
				configurationResourceName = "/" + configurationResourceName;
				LOG.debugf(
						"prepending / to %s. It should be placed in the root of the classpath rather than in a package.",
						configurationResourceName);
			}
			url = loadResource(configurationResourceName);
		}
		return url;
	}

	private Properties loadPropertiesFromURL(URL url) {
		Properties prop = null;
		if (url != null) {
			try (InputStream in = url.openStream(); Reader reader = new InputStreamReader(in, "UTF-8");) {
				prop = new Properties();
				prop.load(reader);
			} catch (IOException e) {
				LOG.error(e);				
			}
		}
		return prop;
	}

	@SuppressWarnings("deprecation")
	private void initRedissonSentinelClient(Properties prop) {
		Config redissonConfig = new Config();
		FstCodec codec = new FstCodec();
		LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

		redissonConfig.setCodec(codec);
		redissonConfig.useSentinelServers()
				.setIdleConnectionTimeout(getIntProperty(prop, REDIS_SERVER_IDLE_CONNECTION_TIMEOUT))
				.setPingConnectionInterval(getIntProperty(prop, REDIS_SERVER_PING_INTERVAL))
				.setPingTimeout(getIntProperty(prop, REDIS_SERVER_PING_TIMEOUT))
				.setConnectTimeout(getIntProperty(prop, REDIS_SERVER_CONNECT_TIMEOUT))
				.setTimeout(getIntProperty(prop, REDIS_SERVER_TIMEOUT))
				.setRetryAttempts(getIntProperty(prop, REDIS_SERVER_RETRY_ATTEMPTS))
				.setReconnectionTimeout(getIntProperty(prop, REDIS_SERVER_RECONNECTION_TIMEOUT))
				.setFailedAttempts(getIntProperty(prop, REDIS_SERVER_FAILED_ATTEMPTS))
				.setSubscriptionsPerConnection(getIntProperty(prop, REDIS_SERVER_SUBSCRIPTION_PER_CONNECTION))
				.setClientName(getProperty(prop, REDIS_SENTINEL_SERVER_CLIENT_NAME)).setLoadBalancer(loadBalancer)
				.setSubscriptionConnectionMinimumIdleSize(getIntProperty(prop, REDIS_SERVER_SUBS_CONN_MIN_IDLE_SIZE))
				.setSubscriptionConnectionPoolSize(getIntProperty(prop, REDIS_SERVER_SUBS_CONN_POOL_SIZE))
				.setSlaveConnectionMinimumIdleSize(getIntProperty(prop, REDIS_SENTINEL_SLAVE_CONNECTION_MIN_IDLE_SIZE))
				.setSlaveConnectionPoolSize(getIntProperty(prop, REDIS_SENTINEL_SLAVE_CONNECTION_POOL_SIZE))
				.setMasterConnectionMinimumIdleSize(
						getIntProperty(prop, REDIS_SENTINEL_MASTER_CONNECTION_MIN_IDLE_SIZE))
				.setMasterConnectionPoolSize(getIntProperty(prop, REDIS_SENTINEL_MASTER_CONNECTION_POOL_SIZE))
				.setMasterName(getProperty(prop, REDIS_SENTINEL_SERVER_MASTER_NAME)).setReadMode(ReadMode.SLAVE)
				.setSubscriptionMode(SubscriptionMode.SLAVE).setDatabase(getIntProperty(prop, REDIS_SERVER_DATABASE_ID))
				.setPassword(getProperty(prop, REDIS_SERVER_PASSWORD_KEY)).addSentinelAddress(getSentinelAddress(prop));

		redisson = Redisson.create(redissonConfig);
		batchOptions = getDefaultBatchOptions(prop);
		localCachedMapOptions = getDefaultLocalCachedMapOptions();
	}

	@SuppressWarnings("deprecation")
	private void initRedissonSingleServerClient(Properties prop) {

		Config redissonConfig = new Config();
		FstCodec codec = new FstCodec();
		redissonConfig.setCodec(codec);
		redissonConfig.useSingleServer()
				.setIdleConnectionTimeout(getIntProperty(prop, REDIS_SERVER_IDLE_CONNECTION_TIMEOUT))
				.setPingConnectionInterval(getIntProperty(prop, REDIS_SERVER_PING_INTERVAL))
				.setPingTimeout(getIntProperty(prop, REDIS_SERVER_PING_TIMEOUT))
				.setConnectTimeout(getIntProperty(prop, REDIS_SERVER_CONNECT_TIMEOUT))
				.setTimeout(getIntProperty(prop, REDIS_SERVER_TIMEOUT))
				.setRetryAttempts(getIntProperty(prop, REDIS_SERVER_RETRY_ATTEMPTS))
				.setReconnectionTimeout(getIntProperty(prop, REDIS_SERVER_RECONNECTION_TIMEOUT))
				.setFailedAttempts(getIntProperty(prop, REDIS_SERVER_FAILED_ATTEMPTS))
				.setSubscriptionsPerConnection(getIntProperty(prop, REDIS_SERVER_SUBSCRIPTION_PER_CONNECTION))
				.setClientName(getProperty(prop, REDIS_SINGLE_SERVER_CLIENT_NAME))
				.setAddress(getProperty(prop, REDIS_SINGLE_SERVER_ADDRESS))
				.setSubscriptionConnectionMinimumIdleSize(getIntProperty(prop, REDIS_SERVER_SUBS_CONN_MIN_IDLE_SIZE))
				.setSubscriptionConnectionPoolSize(getIntProperty(prop, REDIS_SERVER_SUBS_CONN_POOL_SIZE))
				.setConnectionMinimumIdleSize(getIntProperty(prop, REDIS_SINGLE_SERVER_CONNECTION_MIN_IDLE_SIZE))
				.setConnectionPoolSize(getIntProperty(prop, REDIS_SINGLE_SERVER_CONNECTION_POOL_SIZE))
				.setDatabase(getIntProperty(prop, REDIS_SERVER_DATABASE_ID))
				.setPassword(getProperty(prop, REDIS_SERVER_PASSWORD_KEY));

		redisson = Redisson.create(redissonConfig);
		batchOptions = getDefaultBatchOptions(prop);
		localCachedMapOptions = getDefaultLocalCachedMapOptions();

	}

	private int getIntProperty(Properties prop, String key) {
		return Integer.parseInt(getProperty(prop, key));
	}

	private String getProperty(Properties prop, String key) {
		String value = System.getProperty(key);
		if (value == null || value.length() <= 0) {
			value = prop.getProperty(key);
		}
		return (value == null || value.length() <= 0) ? null : value;
	}

	private String[] getSentinelAddress(Properties prop) {
		Set<String> strSet = new HashSet<>();
		strSet = addToStringSet(getProperty(prop, REDIS_SENTINEL_SERVER_ADDRESS_A), strSet);
		strSet = addToStringSet(getProperty(prop, REDIS_SENTINEL_SERVER_ADDRESS_B), strSet);
		strSet = addToStringSet(getProperty(prop, REDIS_SENTINEL_SERVER_ADDRESS_C), strSet);
		strSet = addToStringSet(getProperty(prop, REDIS_SENTINEL_SERVER_ADDRESS_D), strSet);

		int index = -1;
		String[] ret = new String[strSet.size()];

		for (String str : strSet) {
			index++;
			ret[index] = str;
		}
		return ret;
	}
	
	private Set<String> addToStringSet(String temp, Set<String> strSet) {
		if (temp != null && temp.length() > 0) {
			strSet.add(temp);
		}
		return strSet;
	}

	private BatchOptions getDefaultBatchOptions(Properties prop) {
		return BatchOptions.defaults()
				.responseTimeout(getIntProperty(prop, REDIS_SERVER_BATCH_RESPONSE_TIMEOUT), TimeUnit.SECONDS)
				.retryAttempts(getIntProperty(prop, REDIS_SERVER_BATCH_RETRY_ATTEMPTS))
				.retryInterval(getIntProperty(prop, REDIS_SERVER_BATCH_RETRY_INTERVAL), TimeUnit.SECONDS)
				.executionMode(ExecutionMode.IN_MEMORY_ATOMIC);
	}

	private LocalCachedMapOptions<Object, Object> getDefaultLocalCachedMapOptions() {
		return LocalCachedMapOptions.defaults().syncStrategy(SyncStrategy.UPDATE)
				.reconnectionStrategy(ReconnectionStrategy.CLEAR);
	}

}