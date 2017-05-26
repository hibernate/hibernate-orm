/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.h2geodb;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.sqm.produce.function.spi.StandardSqmFunctionTemplate;
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
		registerColumnType( GeoDBGeometryTypeDescriptor.INSTANCE.getJdbcTypeCode(), "GEOMETRY" );

		// Register functions that operate on spatial types
		registerFunction( "dimension", new StandardSqmFunctionTemplate( "ST_Dimension", StandardBasicTypes.INTEGER ) );
		registerFunction( "geometrytype", new StandardSqmFunctionTemplate( "GeometryType", StandardBasicTypes.STRING ) );
		registerFunction( "srid", new StandardSqmFunctionTemplate( "ST_SRID", StandardBasicTypes.INTEGER ) );
		registerFunction( "envelope", new StandardSqmFunctionTemplate( "ST_Envelope" ) );
		registerFunction( "astext", new StandardSqmFunctionTemplate( "ST_AsText", StandardBasicTypes.STRING ) );
		registerFunction( "asbinary", new StandardSqmFunctionTemplate( "ST_AsEWKB", StandardBasicTypes.BINARY ) );
		registerFunction( "isempty", new StandardSqmFunctionTemplate( "ST_IsEmpty", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "issimple", new StandardSqmFunctionTemplate( "ST_IsSimple", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "boundary", new StandardSqmFunctionTemplate( "ST_Boundary" ) );

		// Register functions for spatial relation constructs
		registerFunction( "overlaps", new StandardSqmFunctionTemplate( "ST_Overlaps", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "intersects", new StandardSqmFunctionTemplate( "ST_Intersects", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "equals", new StandardSqmFunctionTemplate( "ST_Equals", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "contains", new StandardSqmFunctionTemplate( "ST_Contains", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "crosses", new StandardSqmFunctionTemplate( "ST_Crosses", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "disjoint", new StandardSqmFunctionTemplate( "ST_Disjoint", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "touches", new StandardSqmFunctionTemplate( "ST_Touches", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "within", new StandardSqmFunctionTemplate( "ST_Within", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "relate", new StandardSqmFunctionTemplate( "ST_Relate", StandardBasicTypes.BOOLEAN ) );
		// register the spatial analysis functions
		registerFunction( "distance", new StandardSqmFunctionTemplate( "ST_Distance", StandardBasicTypes.DOUBLE ) );
		registerFunction( "buffer", new StandardSqmFunctionTemplate( "ST_Buffer" ) );
		registerFunction( "convexhull", new StandardSqmFunctionTemplate( "ST_ConvexHull" ) );
		registerFunction( "difference", new StandardSqmFunctionTemplate( "ST_Difference" ) );
		registerFunction( "intersection", new StandardSqmFunctionTemplate( "ST_Intersection" ) );
		registerFunction( "symdifference", new StandardSqmFunctionTemplate( "ST_SymDifference" ) );
		registerFunction( "geomunion", new StandardSqmFunctionTemplate( "ST_Union" ) );

		registerFunction( "dwithin", new StandardSqmFunctionTemplate( "ST_DWithin", StandardBasicTypes.BOOLEAN ) );

	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		typeContributions.contributeType( new GeolatteGeometryType( GeoDBGeometryTypeDescriptor.INSTANCE, typeContributions.getTypeDescriptorRegistryAccess() ) );
		typeContributions.contributeType( new JTSGeometryType( GeoDBGeometryTypeDescriptor.INSTANCE, typeContributions.getTypeDescriptorRegistryAccess() ) );
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
						"Aggregations of type " + aggregation + " are not supported by " +
								"this dialect"
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
				throw new IllegalArgumentException( "Spatial relation is not known by this dialect" );
		}
	}

	@Override
	public boolean supportsFiltering() {
		return false;
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return function != SpatialFunction.difference && (getFunctions().get( function.toString() ) != null);
	}

}
