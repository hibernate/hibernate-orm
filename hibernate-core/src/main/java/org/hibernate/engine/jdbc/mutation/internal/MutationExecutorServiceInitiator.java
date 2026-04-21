/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator for the {@link MutationExecutorService} service
 *
 * @author Steve Ebersole
 */
public class MutationExecutorServiceInitiator implements StandardServiceInitiator<MutationExecutorService> {
	/**
	 * Singleton access
	 */
	public static final MutationExecutorServiceInitiator INSTANCE = new MutationExecutorServiceInitiator();

	/**
	 * Names the BatchBuilder implementation to use.
	 */
	public static final String EXECUTOR_KEY = "hibernate.jdbc.mutation.executor";

	@Override
	public Class<MutationExecutorService> getServiceInitiated() {
		return MutationExecutorService.class;
	}

	@Override
	public MutationExecutorService initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Object custom = configurationValues.get( EXECUTOR_KEY );
		final var classLoaderService = registry.requireService( ClassLoaderService.class );

		if ( custom == null ) {
			final MutationExecutorService discovered = discover( classLoaderService );
			return discovered != null
					? discovered
					: createStandardService( configurationValues );
		}
		else if ( custom instanceof MutationExecutorService mutationExecutorService ) {
			return mutationExecutorService;
		}
		else {
			final Class<? extends MutationExecutorService> customImplClass;
			if ( custom instanceof Class ) {
				//noinspection unchecked
				customImplClass = (Class<? extends MutationExecutorService>) custom;
			}
			else {
				customImplClass = classLoaderService.classForName( custom.toString() );
			}

			try {
				return customImplClass.getConstructor().newInstance();
			}
			catch (NoSuchMethodException e) {
				throw new HibernateException(
						"Could not locate appropriate MutationExecutorService constructor : " + customImplClass.getName(),
						e );
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to instantiate custom MutationExecutorService : " + customImplClass.getName(), e );
			}
		}
	}

	private static MutationExecutorService discover(ClassLoaderService classLoaderService) {
		final var discovered = classLoaderService.loadJavaServices( MutationExecutorService.class );
		final var iterator = discovered.iterator();
		if ( iterator.hasNext() ) {
			final var selected = iterator.next();
			if ( iterator.hasNext() ) {
				throw new HibernateException(
						"Multiple MutationExecutorService service registrations found via ServiceLoader; "
						+ "specify one explicitly via '" + EXECUTOR_KEY + "'" );
			}
			return selected;
		}
		else {
			return null;
		}
	}

	private MutationExecutorService createStandardService(Map<String, Object> configurationValues) {
		return new StandardMutationExecutorService( configurationValues );
	}
}
