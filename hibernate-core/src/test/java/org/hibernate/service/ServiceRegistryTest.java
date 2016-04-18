package org.hibernate.service;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-10427")
public class ServiceRegistryTest {
	private final ServiceRegistry registry = buildRegistry();
	private final static int NUMBER_OF_THREADS = 100;

	@Test
	public void testOnlyOneInstanceOfTheServiceShouldBeCreated() throws InterruptedException, ExecutionException {

		Future<SlowInitializationService>[] serviceIdentities = execute();

		SlowInitializationService previousResult = null;
		for ( Future<SlowInitializationService> future : serviceIdentities ) {
			final SlowInitializationService result = future.get();
			if ( previousResult == null ) {
				previousResult = result;
			}
			else {
				assertTrue( "There are more than one instance of the service", result == previousResult );
			}
			assertTrue( "The service is not initialized", result.isInitialized() );
			assertTrue( "The service is not configured", result.isConfigured() );
			assertTrue( "The service is not started", result.isStarted() );
		}
	}

	private ServiceRegistry buildRegistry() {
		return new StandardServiceRegistryBuilder()
				.addInitiator( new SlowServiceInitiator() ).build();
	}

	private FutureTask<SlowInitializationService>[] execute()
			throws InterruptedException, ExecutionException {
		FutureTask<SlowInitializationService>[] results = new FutureTask[NUMBER_OF_THREADS];
		ExecutorService executor = Executors.newFixedThreadPool( NUMBER_OF_THREADS );
		for ( int i = 0; i < NUMBER_OF_THREADS; i++ ) {
			results[i] = new FutureTask<SlowInitializationService>( new ServiceCallable( registry ) );
			executor.execute( results[i] );
		}
		return results;
	}

	public class ServiceCallable implements Callable<SlowInitializationService> {
		private final ServiceRegistry registry;

		public ServiceCallable(ServiceRegistry registry) {
			this.registry = registry;
		}

		@Override
		public SlowInitializationService call() throws Exception {
			return registry.getService( SlowInitializationService.class );
		}
	}

	public class SlowInitializationService implements ServiceRegistryAwareService, Configurable, Startable, Service {
		private final static int TIME_TO_SLEEP = 100;
		private boolean initialized;
		private boolean configured;
		private boolean started;

		public SlowInitializationService() {
			try {
				Thread.sleep( TIME_TO_SLEEP );
			}
			catch (InterruptedException e) {
			}
		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {
			try {
				Thread.sleep( TIME_TO_SLEEP );
			}
			catch (InterruptedException e) {
			}

			initialized = true;
		}

		@Override
		public void configure(Map configurationValues) {
			try {
				Thread.sleep( TIME_TO_SLEEP );
			}
			catch (InterruptedException e) {
			}

			configured = true;
		}

		@Override
		public void start() {
			try {
				Thread.sleep( TIME_TO_SLEEP );
			}
			catch (InterruptedException e) {
			}

			started = true;
		}

		public boolean isInitialized() {
			return initialized;
		}

		public boolean isConfigured() {
			return configured;
		}

		public boolean isStarted() {
			return started;
		}
	}

	public class SlowServiceInitiator implements StandardServiceInitiator<SlowInitializationService> {

		@Override
		public Class<SlowInitializationService> getServiceInitiated() {
			return SlowInitializationService.class;
		}

		@Override
		public SlowInitializationService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			return new SlowInitializationService();
		}
	}

}
