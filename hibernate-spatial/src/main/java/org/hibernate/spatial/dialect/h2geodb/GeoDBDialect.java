/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.h2geodb;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
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
		registerFunction( "dimension", new NamedSqmFunctionTemplate( "ST_Dimension", StandardBasicTypes.INTEGER ) );
		registerFunction( "geometrytype", new NamedSqmFunctionTemplate( "GeometryType", StandardBasicTypes.STRING ) );
		registerFunction( "srid", new NamedSqmFunctionTemplate( "ST_SRID", StandardBasicTypes.INTEGER ) );
		registerFunction( "envelope", new NamedSqmFunctionTemplate( "ST_Envelope" ) );
		registerFunction( "astext", new NamedSqmFunctionTemplate( "ST_AsText", StandardBasicTypes.STRING ) );
		registerFunction( "asbinary", new NamedSqmFunctionTemplate( "ST_AsEWKB", StandardBasicTypes.BINARY ) );
		registerFunction( "isempty", new NamedSqmFunctionTemplate( "ST_IsEmpty", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "issimple", new NamedSqmFunctionTemplate( "ST_IsSimple", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "boundary", new NamedSqmFunctionTemplate( "ST_Boundary" ) );

		// Register functions for spatial relation constructs
		registerFunction( "overlaps", new NamedSqmFunctionTemplate( "ST_Overlaps", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "intersects", new NamedSqmFunctionTemplate( "ST_Intersects", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "equals", new NamedSqmFunctionTemplate( "ST_Equals", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "contains", new NamedSqmFunctionTemplate( "ST_Contains", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "crosses", new NamedSqmFunctionTemplate( "ST_Crosses", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "disjoint", new NamedSqmFunctionTemplate( "ST_Disjoint", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "touches", new NamedSqmFunctionTemplate( "ST_Touches", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "within", new NamedSqmFunctionTemplate( "ST_Within", StandardBasicTypes.BOOLEAN ) );
		registerFunction( "relate", new NamedSqmFunctionTemplate( "ST_Relate", StandardBasicTypes.BOOLEAN ) );
		// register the spatial analysis functions
		registerFunction( "distance", new NamedSqmFunctionTemplate( "ST_Distance", StandardBasicTypes.DOUBLE ) );
		registerFunction( "buffer", new NamedSqmFunctionTemplate( "ST_Buffer" ) );
		registerFunction( "convexhull", new NamedSqmFunctionTemplate( "ST_ConvexHull" ) );
		registerFunction( "difference", new NamedSqmFunctionTemplate( "ST_Difference" ) );
		registerFunction( "intersection", new NamedSqmFunctionTemplate( "ST_Intersection" ) );
		registerFunction( "symdifference", new NamedSqmFunctionTemplate( "ST_SymDifference" ) );
		registerFunction( "geomunion", new NamedSqmFunctionTemplate( "ST_Union" ) );

		registerFunction( "dwithin", new NamedSqmFunctionTemplate( "ST_DWithin", StandardBasicTypes.BOOLEAN ) );

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
