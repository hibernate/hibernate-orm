/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.sqlserver;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.contributor.ContributorImplementor;

public class SqlServerDialectContributor implements ContributorImplementor {
	private final ServiceRegistry serviceRegistry;

	public SqlServerDialectContributor(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void contributeJdbcTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		HSMessageLogger.SPATIAL_MSG_LOGGER.typeContributions( this.getClass().getCanonicalName() );
		typeContributions.contributeJdbcType( SqlServerGeometryType.INSTANCE );
		typeContributions.contributeJdbcType( SqlServerGeographyType.INSTANCE );
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		HSMessageLogger.SPATIAL_MSG_LOGGER.functionContributions( this.getClass().getCanonicalName() );
		final SqlServerSqmFunctionDescriptors functions = new SqlServerSqmFunctionDescriptors( functionContributions );
		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		functions.asMap().forEach( (key, desc) -> {
			functionRegistry.register( key.getName(), desc );
			key.getAltName().ifPresent( altName -> functionRegistry.registerAlternateKey( altName, key.getName() ) );
		} );
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
}
