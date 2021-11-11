/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.contributor.ContributorImplementor;

public class PostgisDialectContributor implements ContributorImplementor {

	private final ServiceRegistry serviceRegistryegistry;

	public PostgisDialectContributor(ServiceRegistry serviceRegistry) {
		this.serviceRegistryegistry = serviceRegistry;
	}

	public void contributeTypes(TypeContributions typeContributions) {
		HSMessageLogger.LOGGER.typeContributions( this.getClass().getCanonicalName() );
//		typeContributions.contributeType( new GeolatteGeometryType( PGGeometryJdbcType.INSTANCE_WKB_2 ) );
//		typeContributions.contributeType( new JTSGeometryType( PGGeometryJdbcType.INSTANCE_WKB_2 ) );
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.INSTANCE );
		typeContributions.contributeJdbcTypeDescriptor( PGGeometryJdbcType.INSTANCE_WKB_2 );
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
