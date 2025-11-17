/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.cfg.BatchSettings.BATCH_STRATEGY;
import static org.hibernate.cfg.BatchSettings.BUILDER;
import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInt;

/**
 * Initiator for the {@link BatchBuilder} service
 *
 * @author Steve Ebersole
 */
public class BatchBuilderInitiator implements StandardServiceInitiator<BatchBuilder> {
	/**
	 * Singleton access
	 */
	public static final BatchBuilderInitiator INSTANCE = new BatchBuilderInitiator();

	@Override
	public Class<BatchBuilder> getServiceInitiated() {
		return BatchBuilder.class;
	}

	@Override
	public BatchBuilder initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		Object builder = configurationValues.get( BUILDER );

		if ( builder == null ) {
			builder = configurationValues.get( BATCH_STRATEGY );
		}

		if ( builder == null ) {
			return new BatchBuilderImpl( getInt( STATEMENT_BATCH_SIZE, configurationValues, 1 ) );
		}

		if ( builder instanceof BatchBuilder batchBuilder ) {
			return batchBuilder;
		}

		final String builderClassName = builder.toString();
		try {
			return (BatchBuilder)
					registry.requireService( ClassLoaderService.class )
							.classForName( builderClassName )
							.getConstructor()
							.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not build explicit BatchBuilder [" + builderClassName + "]", e );
		}
	}
}
