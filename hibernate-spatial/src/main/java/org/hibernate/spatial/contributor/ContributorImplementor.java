/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.contributor;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaType;
import org.hibernate.spatial.JTSGeometryJavaType;

/**
 * Internal contract for Type and Function Contributors
 */
public interface ContributorImplementor {

	default void contributeJavaTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeJavaType( GeolatteGeometryJavaType.GEOMETRY_INSTANCE );
		typeContributions.contributeJavaType( GeolatteGeometryJavaType.POINT_INSTANCE );
		typeContributions.contributeJavaType( GeolatteGeometryJavaType.LINESTRING_INSTANCE );
		typeContributions.contributeJavaType( GeolatteGeometryJavaType.POLYGON_INSTANCE );
		typeContributions.contributeJavaType( GeolatteGeometryJavaType.MULTIPOINT_INSTANCE );
		typeContributions.contributeJavaType( GeolatteGeometryJavaType.MULTILINESTRING_INSTANCE );
		typeContributions.contributeJavaType( GeolatteGeometryJavaType.MULTIPOLYGON_INSTANCE );
		typeContributions.contributeJavaType( GeolatteGeometryJavaType.GEOMETRYCOLL_INSTANCE );


		typeContributions.contributeJavaType( JTSGeometryJavaType.GEOMETRY_INSTANCE );
		typeContributions.contributeJavaType( JTSGeometryJavaType.POINT_INSTANCE );
		typeContributions.contributeJavaType( JTSGeometryJavaType.LINESTRING_INSTANCE );
		typeContributions.contributeJavaType( JTSGeometryJavaType.POLYGON_INSTANCE );
		typeContributions.contributeJavaType( JTSGeometryJavaType.MULTIPOINT_INSTANCE );
		typeContributions.contributeJavaType( JTSGeometryJavaType.MULTILINESTRING_INSTANCE );
		typeContributions.contributeJavaType( JTSGeometryJavaType.MULTIPOLYGON_INSTANCE );
		typeContributions.contributeJavaType( JTSGeometryJavaType.GEOMETRYCOLL_INSTANCE );

	}

	void contributeJdbcTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry);

	void contributeFunctions(FunctionContributions functionContributions);

	ServiceRegistry getServiceRegistry();
}
