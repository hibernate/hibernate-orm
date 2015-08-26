package org.hibernate.test.cache.infinispan.util;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.infinispan.commons.executors.CachedThreadPoolExecutorFactory;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.test.fwk.TestResourceTracker;
import org.infinispan.transaction.TransactionMode;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory that should be overridden in tests.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TestInfinispanRegionFactory extends InfinispanRegionFactory {
	private static AtomicInteger counter = new AtomicInteger();

	@Override
	protected EmbeddedCacheManager createCacheManager(ConfigurationBuilderHolder holder) {
		amendConfiguration(holder);
		return new DefaultCacheManager(holder, true);
	}

	protected void amendConfiguration(ConfigurationBuilderHolder holder) {
		holder.getGlobalConfigurationBuilder().globalJmxStatistics().allowDuplicateDomains(true);
		TransportConfigurationBuilder transport = holder.getGlobalConfigurationBuilder().transport();
		transport.nodeName(TestResourceTracker.getNextNodeName());
		transport.clusterName(TestResourceTracker.getCurrentTestName());
		// minimize number of threads using unlimited cached thread pool
		transport.remoteCommandThreadPool().threadPoolFactory(CachedThreadPoolExecutorFactory.create());
		transport.transportThreadPool().threadPoolFactory(CachedThreadPoolExecutorFactory.create());
		for (Map.Entry<String, ConfigurationBuilder> cfg : holder.getNamedConfigurationBuilders().entrySet()) {
			amendCacheConfiguration(cfg.getKey(), cfg.getValue());
		}
	}

	private String buildNodeName() {
		StringBuilder sb = new StringBuilder("Node");
		int id = counter.getAndIncrement();
		int alphabet = 'Z' - 'A';
		do {
			sb.append((char) (id % alphabet + 'A'));
			id /= alphabet;
		} while (id > alphabet);
		return sb.toString();
	}

	protected void amendCacheConfiguration(String cacheName, ConfigurationBuilder configurationBuilder) {
	}

	public static class Transactional extends TestInfinispanRegionFactory {
		@Override
		protected void amendCacheConfiguration(String cacheName, ConfigurationBuilder configurationBuilder) {
			if (!cacheName.endsWith("query") && !cacheName.equals(DEF_TIMESTAMPS_RESOURCE)) {
				configurationBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).useSynchronization(true);
			}
		}
	}
}
