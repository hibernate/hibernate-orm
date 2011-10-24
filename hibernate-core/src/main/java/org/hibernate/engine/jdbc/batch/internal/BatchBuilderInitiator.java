/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator for the {@link BatchBuilder} service
 *
 * @author Steve Ebersole
 */
public class BatchBuilderInitiator implements BasicServiceInitiator<BatchBuilder> {
	public static final BatchBuilderInitiator INSTANCE = new BatchBuilderInitiator();
	public static final String BUILDER = "hibernate.jdbc.batch.builder";

	@Override
	public Class<BatchBuilder> getServiceInitiated() {
		return BatchBuilder.class;
	}

	@Override
	public BatchBuilder initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object builder = configurationValues.get( BUILDER );
		if ( builder == null ) {
			return new BatchBuilderImpl(
					ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, 1 )
			);
		}

		if ( BatchBuilder.class.isInstance( builder ) ) {
			return (BatchBuilder) builder;
		}

		final String builderClassName = builder.toString();
		try {
			return (BatchBuilder) registry.getService( ClassLoaderService.class ).classForName( builderClassName ).newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not build explicit BatchBuilder [" + builderClassName + "]", e );
		}
	}
}
