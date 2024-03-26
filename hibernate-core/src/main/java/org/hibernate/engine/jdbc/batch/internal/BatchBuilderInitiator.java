/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

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
		Object builder = configurationValues.get( BatchSettings.BUILDER );

		if ( builder == null ) {
			builder = configurationValues.get( BatchSettings.BATCH_STRATEGY );
		}

		if ( builder == null ) {
			return new BatchBuilderImpl(
					ConfigurationHelper.getInt( BatchSettings.STATEMENT_BATCH_SIZE, configurationValues, 1 )
			);
		}

		if ( builder instanceof BatchBuilder ) {
			return (BatchBuilder) builder;
		}

		final String builderClassName = builder.toString();
		try {
			return (BatchBuilder) registry.requireService( ClassLoaderService.class )
					.classForName( builderClassName )
					.getConstructor()
					.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not build explicit BatchBuilder [" + builderClassName + "]", e );
		}
	}
}
