/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectLogging;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * Standard implementation of the {@link DialectFactory} service.
 *
 * @author Steve Ebersole
 */
public class DialectFactoryImpl implements DialectFactory, ServiceRegistryAwareService {
	private static final Set<String> LEGACY_DIALECTS = Set.of(
			"org.hibernate.community.dialect.DB297Dialect",
			"org.hibernate.community.dialect.DB2390Dialect",
			"org.hibernate.community.dialect.DB2390V8Dialect",
			"org.hibernate.community.dialect.DerbyTenFiveDialect",
			"org.hibernate.community.dialect.DerbyTenSevenDialect",
			"org.hibernate.community.dialect.DerbyTenSixDialect",
			"org.hibernate.community.dialect.DerbyDialect",
			"org.hibernate.community.dialect.MariaDB10Dialect",
			"org.hibernate.community.dialect.MariaDB53Dialect",
			"org.hibernate.community.dialect.MariaDB102Dialect",
			"org.hibernate.community.dialect.MySQL5Dialect",
			"org.hibernate.community.dialect.MySQL55Dialect",
			"org.hibernate.community.dialect.Oracle8iDialect",
			"org.hibernate.community.dialect.Oracle9iDialect",
			"org.hibernate.community.dialect.Oracle10gDialect",
			"org.hibernate.community.dialect.PostgreSQL9Dialect",
			"org.hibernate.community.dialect.PostgreSQL81Dialect",
			"org.hibernate.community.dialect.PostgreSQL82Dialect",
			"org.hibernate.community.dialect.PostgreSQL91Dialect",
			"org.hibernate.community.dialect.PostgreSQL92Dialect",
			"org.hibernate.community.dialect.PostgreSQL93Dialect",
			"org.hibernate.community.dialect.PostgreSQL94Dialect",
			"org.hibernate.community.dialect.PostgreSQL95Dialect",
			"org.hibernate.community.dialect.SQLServer2005Dialect",
			"org.hibernate.community.dialect.Sybase11Dialect",
			"org.hibernate.community.dialect.SybaseASE15Dialect",
			"org.hibernate.community.dialect.SybaseASE157Dialect"
	);

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
	public Dialect buildDialect(Map<String,Object> configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException {
		final Object dialectReference = configValues.get( AvailableSettings.DIALECT );
		Dialect dialect = isEmpty( dialectReference )
				? determineDialect( resolutionInfoSource )
				: constructDialect( dialectReference, resolutionInfoSource );
		logSelectedDialect( dialect );
		return dialect;
	}

	private static void logSelectedDialect(Dialect dialect) {
		DialectLogging.DIALECT_MESSAGE_LOGGER.usingDialect( dialect );

		Class<? extends Dialect> dialectClass = dialect.getClass();
		if ( dialectClass.isAnnotationPresent( Deprecated.class ) ) {
			Class<?> superDialectClass = dialectClass.getSuperclass();
			if ( !superDialectClass.isAnnotationPresent( Deprecated.class )
					&& !superDialectClass.equals( Dialect.class ) ) {
				DEPRECATION_LOGGER.deprecatedDialect( dialectClass.getSimpleName(), superDialectClass.getName() );
			}
			else {
				DEPRECATION_LOGGER.deprecatedDialect( dialectClass.getSimpleName() );
			}
		}
	}

	private boolean isEmpty(Object dialectReference) {
		if ( dialectReference == null ) {
			return true;
		}
		else {
			// the referenced value is not null
			return dialectReference instanceof String string
				// if it is a String, it might still be empty though...
				&& StringHelper.isEmpty( string );
		}
	}

	private Dialect constructDialect(Object dialectReference, DialectResolutionInfoSource resolutionInfoSource) {
		try {
			Dialect dialect = strategySelector.resolveStrategy(
					Dialect.class,
					dialectReference,
					(Dialect) null,
					(dialectClass) -> {
						try {
							try {
								if ( resolutionInfoSource != null ) {
									return dialectClass.getConstructor( DialectResolutionInfo.class ).newInstance(
											resolutionInfoSource.getDialectResolutionInfo()
									);
								}
							}
							catch (NoSuchMethodException nsme) {

							}
							return dialectClass.newInstance();
						}
						catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
							throw new StrategySelectionException(
									String.format( "Could not instantiate named dialect class [%s]", dialectClass.getName() ),
									e
							);
						}
					}
			);
			if ( dialect == null ) {
				throw new HibernateException( "Unable to construct requested dialect [" + dialectReference + "]" );
			}
			else if ( Dialect.class.getPackage() == dialect.getClass().getPackage() ) {
				DEPRECATION_LOGGER.automaticDialect( dialect.getClass().getSimpleName() );
			}
			return dialect;
		}
		catch (StrategySelectionException e) {
			final String dialectFqn = dialectReference.toString();
			if ( LEGACY_DIALECTS.contains( dialectFqn ) ) {
				throw new StrategySelectionException(
						"Couldn't load the dialect class for the 'hibernate.dialect' [" + dialectFqn + "], " +
								"because the application is missing a dependency on the hibernate-community-dialects module. " +
								"Hibernate 6.2 dropped support for database versions that are unsupported by vendors  " +
								"and code for old versions was moved to the hibernate-community-dialects module. " +
								"For further information, read https://in.relation.to/2023/02/15/hibernate-orm-62-db-version-support/",
						e
				);
			}
			throw e;
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to construct requested dialect [" + dialectReference + "]", e );
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
			throw new HibernateException(
					"Unable to determine Dialect without JDBC metadata "
					+ "(please set '" + JdbcSettings.JAKARTA_JDBC_URL + "' for common cases or '" + JdbcSettings.DIALECT + "' when a custom Dialect implementation must be provided)"
			);
		}

		final DialectResolutionInfo info = resolutionInfoSource.getDialectResolutionInfo();
		final Dialect dialect = dialectResolver.resolveDialect( info );

		if ( dialect == null ) {
			throw new HibernateException(
					"Unable to determine Dialect for " + info.getDatabaseName() + " "
					+ info.getDatabaseMajorVersion() + "." + info.getDatabaseMinorVersion()
					+ " (please set 'hibernate.dialect' or register a Dialect resolver)"
			);
		}

		return dialect;
	}
}
