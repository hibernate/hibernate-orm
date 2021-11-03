/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.mysql;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.KeyedSqmFunctionDescriptors;
import org.hibernate.spatial.contributor.ContributorImplementor;

public class MySQLDialectContributor implements ContributorImplementor {

	private final ServiceRegistry serviceRegistry;

	public MySQLDialectContributor(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions) {
		HSMessageLogger.LOGGER.typeContributions( this.getClass().getCanonicalName() );
		typeContributions.contributeType( new GeolatteGeometryType( MySQLGeometryType.INSTANCE ) );
		typeContributions.contributeType( new JTSGeometryType( MySQLGeometryType.INSTANCE ) );
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		HSMessageLogger.LOGGER.functionContributions( this.getClass().getCanonicalName() );
		final KeyedSqmFunctionDescriptors mysqlFunctions = new MySqlSqmFunctionDescriptors( functionContributions );
		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		mysqlFunctions.asMap().forEach( (key, desc) -> {
			functionRegistry.register( key.getName(), desc );
			key.getAltName().ifPresent( altName -> functionRegistry.registerAlternateKey( altName, key.getName() ) );
		} );
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
}
