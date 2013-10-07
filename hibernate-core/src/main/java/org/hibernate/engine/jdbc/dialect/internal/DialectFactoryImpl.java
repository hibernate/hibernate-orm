/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.dialect.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard implementation of the {@link DialectFactory} service.
 *
 * @author Steve Ebersole
 */
public class DialectFactoryImpl implements DialectFactory, ServiceRegistryAwareService {
	private StrategySelector strategySelector;
	private DialectResolver dialectResolver;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.strategySelector = serviceRegistry.getService( StrategySelector.class );
		this.dialectResolver = serviceRegistry.getService( DialectResolver.class );
	}

	/**
	 * Intended only for use from testing.
	 *
	 * @param dialectResolver The DialectResolver to use
	 */
	public void setDialectResolver(DialectResolver dialectResolver) {
		this.dialectResolver = dialectResolver;
	}

	@Override
	public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException {
		final String dialectName = (String) configValues.get( AvailableSettings.DIALECT );
		if ( !StringHelper.isEmpty( dialectName ) ) {
			return constructDialect( dialectName );
		}
		else {
			return determineDialect( resolutionInfoSource );
		}
	}

	private Dialect constructDialect(String dialectName) {
		final Dialect dialect;
		try {
			dialect = strategySelector.resolveStrategy( Dialect.class, dialectName );
			if ( dialect == null ) {
				throw new HibernateException( "Unable to construct requested dialect [" + dialectName+ "]" );
			}
			return dialect;
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to construct requested dialect [" + dialectName+ "]", e );
		}
	}

	/**
	 * Determine the appropriate Dialect to use given the connection.
	 *
	 * @param resolutionInfoSource Access to DialectResolutionInfo used to resolve the Dialect.
	 *
	 * @return The appropriate dialect instance.
	 *
	 * @throws HibernateException No connection given or no resolver could make
	 * the determination from the given connection.
	 */
	private Dialect determineDialect(DialectResolutionInfoSource resolutionInfoSource) {
		if ( resolutionInfoSource == null ) {
			throw new HibernateException( "Access to DialectResolutionInfo cannot be null when 'hibernate.dialect' not set" );
		}

		final DialectResolutionInfo info = resolutionInfoSource.getDialectResolutionInfo();
		final Dialect dialect = dialectResolver.resolveDialect( info );

		if ( dialect == null ) {
			throw new HibernateException(
					"Unable to determine Dialect to use [name=" + info.getDatabaseName() +
							", majorVersion=" + info.getDatabaseMajorVersion() +
							"]; user must register resolver or explicitly set 'hibernate.dialect'"
			);
		}

		return dialect;
	}
}
