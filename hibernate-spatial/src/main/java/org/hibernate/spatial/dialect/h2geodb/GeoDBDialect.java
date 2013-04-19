/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA, Geodan IT b.v.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

	/*
			 Contents of GeoDB's spatial registration script (geodb.sql):

		  CREATE ALIAS AddGeometryColumn for "geodb.GeoDB.AddGeometryColumn"
		  CREATE ALIAS CreateSpatialIndex for "geodb.GeoDB.CreateSpatialIndex"
		  CREATE ALIAS DropGeometryColumn for "geodb.GeoDB.DropGeometryColumn"
		  CREATE ALIAS DropGeometryColumns for "geodb.GeoDB.DropGeometryColumns"
		  CREATE ALIAS DropSpatialIndex for "geodb.GeoDB.DropSpatialIndex"
		  CREATE ALIAS EnvelopeAsText for "geodb.GeoDB.EnvelopeAsText"
		  CREATE ALIAS GeometryType for "geodb.GeoDB.GeometryType"
		  CREATE ALIAS ST_Area FOR "geodb.GeoDB.ST_Area"
		  CREATE ALIAS ST_AsEWKB FOR "geodb.GeoDB.ST_AsEWKB"
		  CREATE ALIAS ST_AsEWKT FOR "geodb.GeoDB.ST_AsEWKT"
		  CREATE ALIAS ST_AsHexEWKB FOR "geodb.GeoDB.ST_AsHexEWKB"
		  CREATE ALIAS ST_AsText FOR "geodb.GeoDB.ST_AsText"
		  CREATE ALIAS ST_BBOX FOR "geodb.GeoDB.ST_BBox"
		  CREATE ALIAS ST_Buffer FOR "geodb.GeoDB.ST_Buffer"
		  CREATE ALIAS ST_Centroid FOR "geodb.GeoDB.ST_Centroid"
		  CREATE ALIAS ST_Crosses FOR "geodb.GeoDB.ST_Crosses"
		  CREATE ALIAS ST_Contains FOR "geodb.GeoDB.ST_Contains"
		  CREATE ALIAS ST_DWithin FOR "geodb.GeoDB.ST_DWithin"
		  CREATE ALIAS ST_Disjoint FOR "geodb.GeoDB.ST_Disjoint"
		  CREATE ALIAS ST_Distance FOR "geodb.GeoDB.ST_Distance"
		  CREATE ALIAS ST_Envelope FOR "geodb.GeoDB.ST_Envelope"
		  CREATE ALIAS ST_Equals FOR "geodb.GeoDB.ST_Equals"
		  CREATE ALIAS ST_GeoHash FOR "geodb.GeoDB.ST_GeoHash"
		  CREATE ALIAS ST_GeomFromEWKB FOR "geodb.GeoDB.ST_GeomFromEWKB"
		  CREATE ALIAS ST_GeomFromEWKT FOR "geodb.GeoDB.ST_GeomFromEWKT"
		  CREATE ALIAS ST_GeomFromText FOR "geodb.GeoDB.ST_GeomFromText"
		  CREATE ALIAS ST_GeomFromWKB FOR "geodb.GeoDB.ST_GeomFromWKB"
		  CREATE ALIAS ST_Intersects FOR "geodb.GeoDB.ST_Intersects"
		  CREATE ALIAS ST_IsEmpty FOR "geodb.GeoDB.ST_IsEmpty"
		  CREATE ALIAS ST_IsSimple FOR "geodb.GeoDB.ST_IsSimple"
		  CREATE ALIAS ST_IsValid FOR "geodb.GeoDB.ST_IsValid"
		  CREATE ALIAS ST_MakePoint FOR "geodb.GeoDB.ST_MakePoint"
		  CREATE ALIAS ST_MakeBox2D FOR "geodb.GeoDB.ST_MakeBox2D"
		  CREATE ALIAS ST_Overlaps FOR "geodb.GeoDB.ST_Overlaps"
		  CREATE ALIAS ST_SRID FOR "geodb.GeoDB.ST_SRID"
		  CREATE ALIAS ST_SetSRID FOR "geodb.GeoDB.ST_SetSRID"
		  CREATE ALIAS ST_Simplify FOR "geodb.GeoDB.ST_Simplify"
		  CREATE ALIAS ST_Touches FOR "geodb.GeoDB.ST_Touches"
		  CREATE ALIAS ST_Within FOR "geodb.GeoDB.ST_Within"
		  CREATE ALIAS Version FOR "geodb.GeoDB.Version"
		  */


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
		// NOT YET AVAILABLE IN GEODB
		//		registerFunction("dimension", new StandardSQLFunction("dimension",
		//				Hibernate.INTEGER));
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
		registerFunction(
				"envelope", new StandardSQLFunction(
				"ST_Envelope"
		)
		);
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
		// NOT YET AVAILABLE IN GEODB
		//		registerFunction("boundary", new StandardSQLFunction("boundary",
		//				new CustomType(GeoDBGeometryUserType.class, null)));

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
		// NOT YET AVAILABLE IN GEODB
		//		registerFunction("relate", new StandardSQLFunction("relate",
		//				Hibernate.BOOLEAN));

		// register the spatial analysis functions
		registerFunction(
				"distance", new StandardSQLFunction(
				"ST_Distance",
				StandardBasicTypes.DOUBLE
		)
		);
		registerFunction(
				"buffer", new StandardSQLFunction(
				"ST_Buffer"
		)
		);
		// NOT YET AVAILABLE IN GEODB
		//		registerFunction("convexhull", new StandardSQLFunction("convexhull",
		//				new CustomType(GeoDBGeometryUserType.class, null)));
		//		registerFunction("difference", new StandardSQLFunction("difference",
		//				new CustomType(GeoDBGeometryUserType.class, null)));
		//		registerFunction("intersection", new StandardSQLFunction(
		//				"intersection", new CustomType(GeoDBGeometryUserType.class, null)));
		//		registerFunction("symdifference",
		//				new StandardSQLFunction("symdifference", new CustomType(
		//						GeoDBGeometryUserType.class, null)));
		//		registerFunction("geomunion", new StandardSQLFunction("geomunion",
		//				new CustomType(GeoDBGeometryUserType.class, null)));

		//register Spatial Aggregate funciton
		// NOT YET AVAILABLE IN GEODB
		//		registerFunction("extent", new StandardSQLFunction("extent",
		//				new CustomType(GeoDBGeometryUserType.class, null)));

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

	public String getDWithinSQL(String columnName) {
		return "ST_DWithin(" + columnName + ",?,?)";
	}

	public String getHavingSridSQL(String columnName) {
		return "( ST_srid(" + columnName + ") = ?)";
	}

	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		String emptyExpr = " ST_IsEmpty(" + columnName + ") ";
		return isEmpty ? emptyExpr : "( NOT " + emptyExpr + ")";
	}

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#getSpatialFilterExpression(java.lang.String)
		  */
	public String getSpatialFilterExpression(String columnName) {
		return "(" + columnName + " && ? ) ";
	}

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#getSpatialRelateSQL(java.lang.String, int, boolean)
		  */

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

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#getDbGeometryTypeName()
		  */

	public String getDbGeometryTypeName() {
		return "GEOM";
	}

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#isTwoPhaseFiltering()
		  */

	public boolean isTwoPhaseFiltering() {
		return false;
	}

	public boolean supportsFiltering() {
		return false;
	}

	public boolean supports(SpatialFunction function) {
		if ( function == SpatialFunction.difference ) {
			return false;
		}
		return ( getFunctions().get( function.toString() ) != null );
	}

}
