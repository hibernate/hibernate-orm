/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.mariadb;

import java.util.Locale;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;

public interface MariaDBSpatialDialectTrait extends SpatialDialect {


	default void delegateContributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeType( new GeolatteGeometryType( MariaDBGeometryTypeDescriptor.INSTANCE ) );
		typeContributions.contributeType( new JTSGeometryType( MariaDBGeometryTypeDescriptor.INSTANCE ) );

		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.INSTANCE );
	}


	SpatialFunctionsRegistry spatialFunctions();

	@Override
	default String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				return " ST_within(" + columnName + ",?)";
			case SpatialRelation.CONTAINS:
				return " ST_contains(" + columnName + ", ?)";
			case SpatialRelation.CROSSES:
				return " ST_crosses(" + columnName + ", ?)";
			case SpatialRelation.OVERLAPS:
				return " ST_overlaps(" + columnName + ", ?)";
			case SpatialRelation.DISJOINT:
				return " ST_disjoint(" + columnName + ", ?)";
			case SpatialRelation.INTERSECTS:
				return " ST_intersects(" + columnName
						+ ", ?)";
			case SpatialRelation.TOUCHES:
				return " ST_touches(" + columnName + ", ?)";
			case SpatialRelation.EQUALS:
				return " ST_equals(" + columnName + ", ?)";
			default:
				throw new IllegalArgumentException(
						"Spatial relation is not known by this dialect"
				);
		}
	}

	@Override
	default String getSpatialFilterExpression(String columnName) {
		return String.format( Locale.ENGLISH, "MBRIntersects(%s,?)", columnName
		);
	}

	@Override
	default String getSpatialAggregateSQL(String columnName, int aggregation) {
		throw new UnsupportedOperationException( "MariaDB has no spatial aggregate functions." );
	}

	@Override
	default String getDWithinSQL(String columnName) {
		throw new UnsupportedOperationException( "MariaDB doesn't support the DWithin function." );
	}

	@Override
	default String getHavingSridSQL(String columnName) {
		return " (ST_SRID(" + columnName + ") = ?) ";
	}

	@Override
	default String getIsEmptySQL(String columnName, boolean isEmpty) {
		final String emptyExpr = " ST_IsEmpty(" + columnName + ") ";
		return isEmpty ? emptyExpr : "( NOT " + emptyExpr + ")";
	}

	@Override
	default boolean supportsFiltering() {
		return true;
	}

	@Override
	default boolean supports(SpatialFunction function) {
		return spatialFunctions().get( function.toString() ) != null;
	}

}

