/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;


import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialAggregate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;

/**
 * A Dialect for Postgresql with support for the Postgis spatial types, functions and operators (release 1.3 or higher)
 *
 * @author Karel Maesen
 */
public class PostgisDialect extends PostgreSQL82Dialect implements SpatialDialect {


	/**
	 * Creates an instance
	 */
	public PostgisDialect() {
		super();
		registerTypesAndFunctions();
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);

		typeContributions.contributeType( new GeolatteGeometryType( PGGeometryTypeDescriptor.INSTANCE ) );
		typeContributions.contributeType( new JTSGeometryType( PGGeometryTypeDescriptor.INSTANCE ) );
	}

	protected void registerTypesAndFunctions() {

		registerColumnType(
				PGGeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		// Registerfunction calls for registering geometry functions:
		// first argument is the OGC standard functionname, second the name as
		// it occurs in the spatial dialect
		registerFunction(
				"dimension", new StandardSQLFunction(
				"st_dimension",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"geometrytype", new StandardSQLFunction(
				"st_geometrytype", StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"srid", new StandardSQLFunction(
				"st_srid",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"envelope", new StandardSQLFunction(
				"st_envelope"
		)
		);
		registerFunction(
				"astext", new StandardSQLFunction(
				"st_astext",
				StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"asbinary", new StandardSQLFunction(
				"st_asbinary",
				StandardBasicTypes.BINARY
		)
		);
		registerFunction(
				"isempty", new StandardSQLFunction(
				"st_isempty",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"issimple", new StandardSQLFunction(
				"st_issimple",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"boundary", new StandardSQLFunction(
				"st_boundary"
		)
		);

		// Register functions for spatial relation constructs
		registerFunction(
				"overlaps", new StandardSQLFunction(
				"st_overlaps",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"intersects", new StandardSQLFunction(
				"st_intersects",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"equals", new StandardSQLFunction(
				"st_equals",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"contains", new StandardSQLFunction(
				"st_contains",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"crosses", new StandardSQLFunction(
				"st_crosses",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"disjoint", new StandardSQLFunction(
				"st_disjoint",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"touches", new StandardSQLFunction(
				"st_touches",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"within", new StandardSQLFunction(
				"st_within",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"relate", new StandardSQLFunction(
				"st_relate",
				StandardBasicTypes.BOOLEAN
		)
		);

		// register the spatial analysis functions
		registerFunction(
				"distance", new StandardSQLFunction(
				"st_distance",
				StandardBasicTypes.DOUBLE
		)
		);
		registerFunction(
				"buffer", new StandardSQLFunction(
				"st_buffer"
		)
		);
		registerFunction(
				"convexhull", new StandardSQLFunction(
				"st_convexhull"
		)
		);
		registerFunction(
				"difference", new StandardSQLFunction(
				"st_difference"
		)
		);
		registerFunction(
				"intersection", new StandardSQLFunction(
				"st_intersection"
		)
		);
		registerFunction(
				"symdifference",
				new StandardSQLFunction( "st_symdifference" )
		);
		registerFunction(
				"geomunion", new StandardSQLFunction(
				"st_union"
		)
		);

		//register Spatial Aggregate function
		registerFunction(
				"extent", new StandardSQLFunction(
				"extent"
		)
		);

		//other common functions
		registerFunction(
				"dwithin", new StandardSQLFunction(
				"st_dwithin",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"transform", new StandardSQLFunction(
				"st_transform"
		)
		);
	}

	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
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
	public String getDWithinSQL(String columnName) {
		return "ST_DWithin(" + columnName + ",?,?)";
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return "( ST_srid(" + columnName + ") = ?)";
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		final String emptyExpr = " ST_IsEmpty(" + columnName + ") ";
		return isEmpty ? emptyExpr : "( NOT " + emptyExpr + ")";
	}

	@Override
	public String getSpatialFilterExpression(String columnName) {
		return "(" + columnName + " && ? ) ";
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		switch ( aggregation ) {
			case SpatialAggregate.EXTENT:
				final StringBuilder stbuf = new StringBuilder();
				stbuf.append( "extent(" ).append( columnName ).append( ")" );
				return stbuf.toString();
			default:
				throw new IllegalArgumentException(
						"Aggregation of type "
								+ aggregation + " are not supported by this dialect"
				);
		}
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return ( getFunctions().get( function.toString() ) != null );
	}
}
