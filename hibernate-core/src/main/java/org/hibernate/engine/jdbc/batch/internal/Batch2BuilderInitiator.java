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
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.spi.Batch2Builder;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator for the {@link Batch2Builder} service
 *
 * @author Steve Ebersole
 */
public class Batch2BuilderInitiator implements StandardServiceInitiator<Batch2Builder> {
	/**
	 * Singleton access
	 */
	public static final Batch2BuilderInitiator INSTANCE = new Batch2BuilderInitiator();

	/**
	 * Names the BatchBuilder implementation to use.
	 */
	public static final String BUILDER = "hibernate.jdbc.batch2.builder";

	@Override
	public Class<Batch2Builder> getServiceInitiated() {
		return Batch2Builder.class;
	}

	@Override
	public Batch2Builder initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Object builder = configurationValues.get( BUILDER );
		if ( builder == null ) {
			return new Batch2BuilderImpl(
					ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, 1 )
			);
		}

		if ( builder instanceof BatchBuilder ) {
			return (Batch2Builder) builder;
		}

		final String builderClassName = builder.toString();
		try {
			return (Batch2Builder) registry.getService( ClassLoaderService.class )
					.classForName( builderClassName )
					.getConstructor()
					.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not build explicit BatchBuilder [" + builderClassName + "]", e );
		}
	}
}
