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
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ServiceRegistryTest {
	private final ServiceRegistry registry = buildRegistry();
	private final static int NUMBER_OF_THREADS = 100;
	private StandardServiceRegistryBuilder standardServiceRegistryBuilder;

	@Test
	@TestForIssue(jiraKey = "HHH-10427")
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

		}

		standardServiceRegistryBuilder.destroy( registry );

	}

	@Test
	@TestForIssue(jiraKey = "HHH-11395")
	public void testGetService() {
		assertThat(
				registry.getService( SlowInitializationService.class ),
				instanceOf( SlowInitializationService.class )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11395")
	public void testGetServiceReturnsNullWhenTheServiceInitiatorInitiateServiceReturnsNull() {
		assertNull( registry.getService( FakeService.class ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11395")
	public void testRequireService() {
		assertThat(
				registry.requireService( SlowInitializationService.class ),
				instanceOf( SlowInitializationService.class )
		);
	}

	@Test(expected = NullServiceException.class)
	@TestForIssue(jiraKey = "HHH-11395")
	public void testRequireServiceThrowsAnExceptionWhenTheServiceInitiatorInitiateServiceReturnsNull() {
		assertNull( registry.requireService( FakeService.class ) );
	}

	private ServiceRegistry buildRegistry() {
		standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
		return standardServiceRegistryBuilder.addInitiator( new SlowServiceInitiator() )
				.addInitiator( new NullServiceInitiator() )
				.build();
	}

	private FutureTask<SlowInitializationService>[] execute()
			throws InterruptedException, ExecutionException {
		FutureTask<SlowInitializationService>[] results = new FutureTask[NUMBER_OF_THREADS];
		ExecutorService executor = Executors.newFixedThreadPool( NUMBER_OF_THREADS );
		for ( int i = 0; i < NUMBER_OF_THREADS; i++ ) {
			results[i] = new FutureTask<>( new ServiceCallable( registry ) );
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
			final SlowInitializationService service = registry.getService( SlowInitializationService.class );
			assertTrue( "The service is not initialized", service.isInitialized() );
			assertTrue( "The service is not configured", service.isConfigured() );
			assertTrue( "The service is not started", service.isStarted() );
			return service;
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

	public class NullServiceInitiator implements StandardServiceInitiator<FakeService> {

		@Override
		public Class<FakeService> getServiceInitiated() {
			return FakeService.class;
		}

		@Override
		public FakeService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			return null;
		}
	}

	public class FakeService implements ServiceRegistryAwareService, Configurable, Startable, Service {

		@Override
		public void start() {

		}

		@Override
		public void configure(Map configurationValues) {

		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {

		}
	}

}
