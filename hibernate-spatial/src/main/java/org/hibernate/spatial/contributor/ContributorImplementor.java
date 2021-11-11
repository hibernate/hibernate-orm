/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.contributor;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;

/**
 * Internal contract for Type and Function Contributors
 */
public interface ContributorImplementor {

	default void contributeJavaTypes(TypeContributions typeContributions) {
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.GEOMETRY_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.POINT_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.LINESTRING_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.POLYGON_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.MULTIPOINT_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.MULTILINESTRING_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.MULTIPOLYGON_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.GEOMETRYCOLL_INSTANCE );


		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.GEOMETRY_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.POINT_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.LINESTRING_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.POLYGON_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.MULTIPOINT_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.MULTILINESTRING_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.MULTIPOLYGON_INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.GEOMETRYCOLL_INSTANCE );

	}

	void contributeJdbcTypes(TypeContributions typeContributions);

	void contributeFunctions(FunctionContributions functionContributions);

	ServiceRegistry getServiceRegistry();
}
