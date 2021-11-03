/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.h2gis;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.contributor.ContributorImplementor;
import org.hibernate.spatial.dialect.postgis.PGGeometryType;
import org.hibernate.spatial.dialect.postgis.PostgisSqmFunctionDescriptors;

public class H2GisDialectContributor implements ContributorImplementor {

	private final ServiceRegistry serviceRegistryegistry;

	public H2GisDialectContributor(ServiceRegistry serviceRegistry) {
		this.serviceRegistryegistry = serviceRegistry;
	}

	public void contributeTypes(TypeContributions typeContributions) {
		HSMessageLogger.LOGGER.typeContributions( this.getClass().getCanonicalName() );
		typeContributions.contributeType( new GeolatteGeometryType( GeoDBGeometryType.INSTANCE ) );
		typeContributions.contributeType( new JTSGeometryType( GeoDBGeometryType.INSTANCE ) );
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		HSMessageLogger.LOGGER.functionContributions( this.getClass().getCanonicalName() );
		final PostgisSqmFunctionDescriptors postgisFunctions = new PostgisSqmFunctionDescriptors( functionContributions );
		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		postgisFunctions.asMap().forEach( (key, desc) -> {
			functionRegistry.register( key.getName(), desc );
			key.getAltName().ifPresent( altName -> functionRegistry.registerAlternateKey( altName, key.getName() ) );
		} );
	}


	@Override
	public ServiceRegistry getServiceRegistry() {
		return this.serviceRegistryegistry;
	}
}
