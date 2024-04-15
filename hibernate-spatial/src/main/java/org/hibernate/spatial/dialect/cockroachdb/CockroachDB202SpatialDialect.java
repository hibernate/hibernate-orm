/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.cockroachdb;

import java.util.Map;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.CockroachDB201Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.dialect.postgis.PGGeometryTypeDescriptor;

/**
 * An @{code SpatialDialect} for CockroachDB 20.2 and later. CockroachDB's spatial features where introduced in
 * that version.
 */
public class CockroachDB202SpatialDialect extends CockroachDB201Dialect implements CockroachSpatialDialectTrait {


	public CockroachDB202SpatialDialect() {
		super();
		registerColumnType(
				PGGeometryTypeDescriptor.INSTANCE_WKB_2.getSqlType(),
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
		delegateContributeTypes( typeContributions, serviceRegistry );
	}

	@Override
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		return super.equivalentTypes( typeCode1, typeCode2 ) ||
				( isSpatial( typeCode1 ) && isSpatial( typeCode2 ) );
	}
}
