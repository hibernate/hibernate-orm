/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.mariadb;

import java.util.Map;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;

public class MariaDB103SpatialDialect extends MariaDB103Dialect implements MariaDBSpatialDialectTrait {

	final private SpatialFunctionsRegistry spatialFunctions = new MariaDB103SpatialFunctions();

	public MariaDB103SpatialDialect() {
		super();
		registerColumnType(
				MariaDBGeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);
		for ( Map.Entry<String, SQLFunction> entry : spatialFunctions ) {
			registerFunction( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		delegateContributeTypes( typeContributions, serviceRegistry );
	}

	@Override
	public SpatialFunctionsRegistry spatialFunctions() {
		return spatialFunctions;
	}
}
