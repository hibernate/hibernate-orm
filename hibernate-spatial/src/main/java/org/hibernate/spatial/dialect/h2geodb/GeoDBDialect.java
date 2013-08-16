/*
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for spatial (geographic) data.
 *
 * Copyright © 2007-2013 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.dialect.h2geodb;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.metamodel.spi.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;

/**
 * Extends the H2Dialect by also including information on spatial functions.
 *
 * @author Jan Boonen, Geodan IT b.v.
 */
public class GeoDBDialect extends H2Dialect implements SpatialDialect {



	/**
	 * Constructor. Registers OGC simple feature functions (see
	 * http://portal.opengeospatial.org/files/?artifact_id=829 for details).
	 * <p/>
	 * Note for the registerfunction method: it registers non-standard database
	 * functions: first argument is the internal (OGC standard) function name,
	 * second the name as it occurs in the spatial dialect
	 */
	public GeoDBDialect() {
		super();

		// Register Geometry column type
		registerColumnType(
				GeoDBGeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);

		// Register functions that operate on spatial types
		registerFunction(
				"dimension", new StandardSQLFunction(
				"ST_Dimension",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction(
				"geometrytype", new StandardSQLFunction(
				"GeometryType", StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"srid", new StandardSQLFunction(
				"ST_SRID",
				StandardBasicTypes.INTEGER
		)
		);
		registerFunction( "envelope", new StandardSQLFunction( "ST_Envelope" ) );
		registerFunction(
				"astext", new StandardSQLFunction(
				"ST_AsText",
				StandardBasicTypes.STRING
		)
		);
		registerFunction(
				"asbinary", new StandardSQLFunction(
				"ST_AsEWKB",
				StandardBasicTypes.BINARY
		)
		);
		registerFunction(
				"isempty", new StandardSQLFunction(
				"ST_IsEmpty",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"issimple", new StandardSQLFunction(
				"ST_IsSimple",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction( "boundary", new StandardSQLFunction( "ST_Boundary" ) );

		// Register functions for spatial relation constructs
		registerFunction(
				"overlaps", new StandardSQLFunction(
				"ST_Overlaps",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"intersects", new StandardSQLFunction(
				"ST_Intersects",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"equals", new StandardSQLFunction(
				"ST_Equals",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"contains", new StandardSQLFunction(
				"ST_Contains",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"crosses", new StandardSQLFunction(
				"ST_Crosses",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"disjoint", new StandardSQLFunction(
				"ST_Disjoint",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"touches", new StandardSQLFunction(
				"ST_Touches",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"within", new StandardSQLFunction(
				"ST_Within",
				StandardBasicTypes.BOOLEAN
		)
		);
		registerFunction(
				"relate", new StandardSQLFunction(
				"ST_Relate",
				StandardBasicTypes.BOOLEAN
		)
		);
		// register the spatial analysis functions
		registerFunction(
				"distance", new StandardSQLFunction(
				"ST_Distance",
				StandardBasicTypes.DOUBLE
		)
		);
		registerFunction( "buffer", new StandardSQLFunction( "ST_Buffer" ) );
		registerFunction( "convexhull", new StandardSQLFunction( "ST_ConvexHull" ) );
		registerFunction( "difference", new StandardSQLFunction( "ST_Difference" ) );
		registerFunction( "intersection", new StandardSQLFunction( "ST_Intersection" ) );
		registerFunction( "symdifference", new StandardSQLFunction( "ST_SymDifference" ) );
		registerFunction( "geomunion", new StandardSQLFunction( "ST_Union" ) );

		registerFunction(
				"dwithin", new StandardSQLFunction(
				"ST_DWithin",
				StandardBasicTypes.BOOLEAN
		)
		);

	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);
		typeContributions.contributeType( new GeolatteGeometryType( GeoDBGeometryTypeDescriptor.INSTANCE ) );
		typeContributions.contributeType( new JTSGeometryType( GeoDBGeometryTypeDescriptor.INSTANCE ) );
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		switch ( aggregation ) {
			// NOT YET AVAILABLE IN GEODB
			//		case SpatialAggregate.EXTENT:
			//			StringBuilder stbuf = new StringBuilder();
			//			stbuf.append("extent(").append(columnName).append(")");
			//			return stbuf.toString();
			default:
				throw new IllegalArgumentException(
						"Aggregations of type "
								+ aggregation + " are not supported by this dialect"
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
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				return " ST_Within(" + columnName + ", ?)";
			case SpatialRelation.CONTAINS:
				return " ST_Contains(" + columnName + ", ?)";
			case SpatialRelation.CROSSES:
				return " ST_Crosses(" + columnName + ", ?)";
			case SpatialRelation.OVERLAPS:
				return " ST_Overlaps(" + columnName + ", ?)";
			case SpatialRelation.DISJOINT:
				return " ST_Disjoint(" + columnName + ", ?)";
			case SpatialRelation.INTERSECTS:
				return " ST_Intersects(" + columnName + ", ?)";
			case SpatialRelation.TOUCHES:
				return " ST_Touches(" + columnName + ", ?)";
			case SpatialRelation.EQUALS:
				return " ST_Equals(" + columnName + ", ?)";
			default:
				throw new IllegalArgumentException(
						"Spatial relation is not known by this dialect"
				);
		}
	}

	@Override
	public boolean supportsFiltering() {
		return false;
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return function != SpatialFunction.difference && ( getFunctions().get( function.toString() ) != null );
	}

}
