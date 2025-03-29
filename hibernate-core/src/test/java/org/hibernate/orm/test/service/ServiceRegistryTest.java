/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.NullServiceException;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ServiceRegistryTest {
	private ServiceRegistry registry;
	private final static int NUMBER_OF_THREADS = 100;

	@Before
	public void init() {
		registry = buildRegistry();
	}

	@After
	public void destroy() {
		registry.close();
	}

	@Test
	@JiraKey(value = "HHH-10427")
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
	}

	@Test
	@JiraKey(value = "HHH-11395")
	public void testGetService() {
		assertThat(
				registry.getService( SlowInitializationService.class ),
				instanceOf( SlowInitializationService.class )
		);
	}

	@Test
	@JiraKey(value = "HHH-11395")
	public void testGetServiceReturnsNullWhenTheServiceInitiatorInitiateServiceReturnsNull() {
		assertNull( registry.getService( FakeService.class ) );
	}

	@Test
	@JiraKey(value = "HHH-11395")
	public void testRequireService() {
		assertThat(
				registry.requireService( SlowInitializationService.class ),
				instanceOf( SlowInitializationService.class )
		);
	}

	@Test(expected = NullServiceException.class)
	@JiraKey(value = "HHH-11395")
	public void testRequireServiceThrowsAnExceptionWhenTheServiceInitiatorInitiateServiceReturnsNull() {
		assertNull( registry.requireService( FakeService.class ) );
	}

	private ServiceRegistry buildRegistry() {
		return ServiceRegistryUtil.serviceRegistryBuilder().addInitiator( new SlowServiceInitiator() )
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
		public void configure(Map<String, Object> configurationValues) {
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
		public SlowInitializationService initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
			return new SlowInitializationService();
		}
	}

	public class NullServiceInitiator implements StandardServiceInitiator<FakeService> {

		@Override
		public Class<FakeService> getServiceInitiated() {
			return FakeService.class;
		}

		@Override
		public FakeService initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
			return null;
		}
	}

	public class FakeService implements ServiceRegistryAwareService, Configurable, Startable, Service {

		@Override
		public void start() {

		}

		@Override
		public void configure(Map<String, Object> configurationValues) {

		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {

		}
	}

}
