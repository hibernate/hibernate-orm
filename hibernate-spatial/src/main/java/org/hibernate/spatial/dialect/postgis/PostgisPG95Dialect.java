/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import java.util.Map;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.ServiceRegistry;

/**
 * Extends the {@code PostgreSQL95Dialect} to add support for the Postgis spatial types, functions and operators .
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 */
public class PostgisPG95Dialect extends PostgreSQL95Dialect implements PGSpatialDialectTrait {

	/**
	 * Creates an instance
	 */
	public PostgisPG95Dialect() {
		super();
		registerColumnType(
				PGGeometryTypeDescriptor.INSTANCE_WKB_1.getSqlType(),
				"GEOMETRY"
		);
		for ( Map.Entry<String, SQLFunction> entry : functionsToRegister() ) {
			registerFunction( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);
		support.contributeTypes( typeContributions, serviceRegistry );
	}

	@Override
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		return super.equivalentTypes( typeCode1, typeCode2 ) ||
				( isSpatial( typeCode1 ) && isSpatial( typeCode2 ) );
	}
}
