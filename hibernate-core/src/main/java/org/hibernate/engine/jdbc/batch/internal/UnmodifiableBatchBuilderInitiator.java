/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator for the {@link UnmodifiableBatchBuilderImpl} service using
 * {@link UnmodifiableBatchBuilderImpl}.
 * This is not the default implementation, but it's a useful alternative to have
 * in some environments.
 *
 * @author Sanne Grinovero
 */
public final class UnmodifiableBatchBuilderInitiator implements StandardServiceInitiator<BatchBuilder> {
	/**
	 * Singleton access
	 */
	public static final UnmodifiableBatchBuilderInitiator INSTANCE = new UnmodifiableBatchBuilderInitiator();

	@Override
	public Class<BatchBuilder> getServiceInitiated() {
		return BatchBuilder.class;
	}

	@Override
	public BatchBuilder initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object builder = configurationValues.get( BatchBuilderInitiator.BUILDER );
		if ( builder == null ) {
			return new UnmodifiableBatchBuilderImpl(
					ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, 1 )
			);
		}
		else {
			throw new ServiceException( "This Hibernate ORM serviceregistry has been configured explicitly to use " + this.getClass() +
												" to create BatchBuilder instances; the property '" + BatchBuilderInitiator.BUILDER
												+ "' is not supported." );
		}
	}
}
