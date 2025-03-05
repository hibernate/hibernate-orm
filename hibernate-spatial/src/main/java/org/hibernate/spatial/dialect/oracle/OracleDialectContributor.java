/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.HibernateSpatialConfigurationSettings;
import org.hibernate.spatial.KeyedSqmFunctionDescriptors;
import org.hibernate.spatial.contributor.ContributorImplementor;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;
import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;

public class OracleDialectContributor implements ContributorImplementor {

	private final ServiceRegistry serviceRegistry;
	private final boolean useSTGeometry;

	public OracleDialectContributor(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		final var cfgService = getServiceRegistry().getService( ConfigurationService.class );
		this.useSTGeometry = cfgService.getSetting(
				HibernateSpatialConfigurationSettings.ORACLE_OGC_STRICT,
				StandardConverters.BOOLEAN,
				false
		);
	}

	@Override
	public void contributeJdbcTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		HSMessageLogger.SPATIAL_MSG_LOGGER.typeContributions( this.getClass().getCanonicalName() );
		final ConfigurationService cfgService = getServiceRegistry().getService( ConfigurationService.class );
		final StrategySelector strategySelector = getServiceRegistry().getService( StrategySelector.class );


		final ConnectionFinder connectionFinder = strategySelector.resolveStrategy(
				ConnectionFinder.class,
				cfgService.getSetting(
						HibernateSpatialConfigurationSettings.CONNECTION_FINDER,
						String.class,
						"org.geolatte.geom.codec.db.oracle.DefaultConnectionFinder"
				)
		);

		HSMessageLogger.SPATIAL_MSG_LOGGER.connectionFinder( connectionFinder.getClass().getCanonicalName() );

		SDOGeometryType sdoGeometryType = new SDOGeometryType(
				new OracleJDBCTypeFactory( connectionFinder ), useSTGeometry
		);

		typeContributions.contributeJdbcType( sdoGeometryType );
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		HSMessageLogger.SPATIAL_MSG_LOGGER.functionContributions( this.getClass().getCanonicalName() );

		KeyedSqmFunctionDescriptors functionDescriptors;
		if ( useSTGeometry ) {
			functionDescriptors = new OracleSQLMMFunctionDescriptors( functionContributions );
		}
		else {
			functionDescriptors = new OracleSDOFunctionDescriptors( functionContributions );
		}
		SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		functionDescriptors.asMap().forEach( (key, funcDescr) -> {
			functionRegistry.register( key.getName(), funcDescr );
			key.getAltName().ifPresent( altName -> functionRegistry.register( altName, funcDescr ) );
		} );

	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
}
