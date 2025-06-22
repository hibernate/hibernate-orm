/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.cockroachdb;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.type.PgJdbcHelper;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.FunctionKey;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.contributor.ContributorImplementor;
import org.hibernate.spatial.dialect.postgis.PGCastingGeographyJdbcType;
import org.hibernate.spatial.dialect.postgis.PGCastingGeometryJdbcType;
import org.hibernate.spatial.dialect.postgis.PGGeographyJdbcType;
import org.hibernate.spatial.dialect.postgis.PGGeometryJdbcType;
import org.hibernate.spatial.dialect.postgis.PostgisSqmFunctionDescriptors;

public class CockroachDbContributor implements ContributorImplementor {

	private final ServiceRegistry serviceRegistry;

	public CockroachDbContributor(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void contributeJdbcTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		HSMessageLogger.SPATIAL_MSG_LOGGER.typeContributions( this.getClass().getCanonicalName() );
		if ( PgJdbcHelper.isUsable( serviceRegistry ) ) {
			typeContributions.contributeJdbcType( PGGeometryJdbcType.INSTANCE_WKB_2 );
			typeContributions.contributeJdbcType( PGGeographyJdbcType.INSTANCE_WKB_2 );
		}
		else {
			typeContributions.contributeJdbcType( PGCastingGeometryJdbcType.INSTANCE_WKB_2 );
			typeContributions.contributeJdbcType( PGCastingGeographyJdbcType.INSTANCE_WKB_2 );
		}
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		HSMessageLogger.SPATIAL_MSG_LOGGER.functionContributions( this.getClass().getCanonicalName() );
		final PostgisSqmFunctionDescriptors postgisFunctions = new PostgisSqmFunctionDescriptors( functionContributions );
		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();

		postgisFunctions.asMap()
				.forEach( (key, desc) -> {
					if ( isUnsupported( key ) ) {
						return;
					}
					functionRegistry.register( key.getName(), desc );
					key.getAltName().ifPresent( altName -> functionRegistry.registerAlternateKey(
							altName,
							key.getName()
					) );
				} );
	}

	private boolean isUnsupported(FunctionKey key) {
		return key.getName().equalsIgnoreCase( "st_union" );
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return this.serviceRegistry;
	}
}
