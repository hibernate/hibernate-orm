/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.SpatialAnalysis;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;
import org.hibernate.spatial.dialect.oracle.criterion.OracleSpatialAggregate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Helper class to register functions in the Oracle Spatial Dialects
 * Created by Karel Maesen, Geovise BVBA on 02/03/16.
 */
class OracleSpatialFunctions extends SpatialFunctionsRegistry {


	OracleSpatialFunctions(boolean strictOgc, OracleSDOSupport sdoSupport) {
		put( "dimension", new GetDimensionFunction() );
		put( "geometrytype", new GetGeometryTypeFunction() );
		put( "srid", new SDOObjectProperty( "SDO_SRID", StandardBasicTypes.INTEGER ) );
		put( "envelope", new StandardSQLFunction( "SDO_GEOM.SDO_MBR" ) );
		put( "astext", new AsTextFunction() );
		put(
				"asbinary",
				new StandardSQLFunction( "SDO_UTIL.TO_WKBGEOMETRY", StandardBasicTypes.BINARY )
		);
		put(
				"isempty",
				new WrappedOGCFunction( "OGC_ISEMPTY", StandardBasicTypes.BOOLEAN, new boolean[] { true } )
		);
		put(
				"issimple",
				new WrappedOGCFunction( "OGC_ISSIMPLE", StandardBasicTypes.BOOLEAN, new boolean[] { true } )
		);
		put( "boundary", new WrappedOGCFunction( "OGC_BOUNDARY", new boolean[] { true } ) );

		// put("area", new AreaFunction());

		// Register functions for spatial relation constructs
		// section 2.1.1.2
		put(
				"overlaps",
				new SpatialRelateFunction( "overlaps", SpatialRelation.OVERLAPS, strictOgc, sdoSupport )
		);
		put(
				"intersects",
				new SpatialRelateFunction( "intersects", SpatialRelation.INTERSECTS, strictOgc, sdoSupport )
		);
		put(
				"contains",
				new SpatialRelateFunction( "contains", SpatialRelation.CONTAINS, strictOgc, sdoSupport )
		);
		put(
				"crosses",
				new SpatialRelateFunction( "crosses", SpatialRelation.CROSSES, strictOgc, sdoSupport )
		);
		put(
				"disjoint",
				new SpatialRelateFunction( "disjoint", SpatialRelation.DISJOINT, strictOgc, sdoSupport )
		);
		put(
				"equals",
				new SpatialRelateFunction( "equals", SpatialRelation.EQUALS, strictOgc, sdoSupport )
		);
		put(
				"touches",
				new SpatialRelateFunction( "touches", SpatialRelation.TOUCHES, strictOgc, sdoSupport )
		);
		put(
				"within",
				new SpatialRelateFunction( "within", SpatialRelation.WITHIN, strictOgc, sdoSupport )
		);
		put(
				"relate",
				new WrappedOGCFunction( "OGC_RELATE", StandardBasicTypes.BOOLEAN, new boolean[] { true, true, false } )
		);

		// Register spatial analysis functions.
		// Section 2.1.1.3
		put(
				"distance",
				new SpatialAnalysisFunction(
						"distance",
						StandardBasicTypes.DOUBLE,
						SpatialAnalysis.DISTANCE,
						strictOgc
				)
		);
		put(
				"buffer",
				new SpatialAnalysisFunction( "buffer", SpatialAnalysis.BUFFER, strictOgc )
		);
		put(
				"convexhull",
				new SpatialAnalysisFunction( "convexhull", SpatialAnalysis.CONVEXHULL, strictOgc )
		);
		put(
				"difference",
				new SpatialAnalysisFunction( "difference", SpatialAnalysis.DIFFERENCE, strictOgc )
		);
		put(
				"intersection",
				new SpatialAnalysisFunction(
						"intersection",
						SpatialAnalysis.INTERSECTION,
						strictOgc
				)
		);
		put(
				"symdifference",
				new SpatialAnalysisFunction(
						"symdifference",
						SpatialAnalysis.SYMDIFFERENCE,
						strictOgc
				)
		);
		put(
				"geomunion",
				new SpatialAnalysisFunction( "union", SpatialAnalysis.UNION, strictOgc )
		);
		// we rename OGC union to geomunion because union is a reserved SQL
		// keyword. (See also postgis documentation).

		// portable spatial aggregate functions
		put(
				"extent",
				new SpatialAggregationFunction( "extent", OracleSpatialAggregate.EXTENT, sdoSupport )
		);

		// spatial filter function
		put(
				SpatialFunction.filter.name(),
				new StandardSQLFunction( "SDO_FILTER" )
		);

		//other common functions

		put( "transform", new StandardSQLFunction( "SDO_CS.TRANSFORM" ) );

		// Oracle specific Aggregate functions
		put(
				"centroid",
				new SpatialAggregationFunction( "extent", OracleSpatialAggregate.CENTROID, sdoSupport )
		);
		put(
				"concat_lines",
				new SpatialAggregationFunction( "extent", OracleSpatialAggregate.CONCAT_LINES, sdoSupport )
		);
		put(
				"aggr_convexhull",
				new SpatialAggregationFunction( "extent", OracleSpatialAggregate.CONVEXHULL, sdoSupport )
		);
		put(
				"aggr_union",
				new SpatialAggregationFunction( "extent", OracleSpatialAggregate.UNION, sdoSupport )
		);
		put(
				"lrs_concat",
				new SpatialAggregationFunction( "lrsconcat", OracleSpatialAggregate.LRS_CONCAT, sdoSupport )
		);

	}

	static String getOGCSpatialAnalysisSQL(List args, int spatialAnalysisFunction) {
		boolean[] geomArgs;
		final StringBuffer ogcFunction = new StringBuffer( "MDSYS." );
		boolean isGeomReturn = true;
		switch ( spatialAnalysisFunction ) {
			case SpatialAnalysis.BUFFER:
				ogcFunction.append( "OGC_BUFFER" );
				geomArgs = new boolean[] { true, false };
				break;
			case SpatialAnalysis.CONVEXHULL:
				ogcFunction.append( "OGC_CONVEXHULL" );
				geomArgs = new boolean[] { true };
				break;
			case SpatialAnalysis.DIFFERENCE:
				ogcFunction.append( "OGC_DIFFERENCE" );
				geomArgs = new boolean[] { true, true };
				break;
			case SpatialAnalysis.DISTANCE:
				ogcFunction.append( "OGC_DISTANCE" );
				geomArgs = new boolean[] { true, true };
				isGeomReturn = false;
				break;
			case SpatialAnalysis.INTERSECTION:
				ogcFunction.append( "OGC_INTERSECTION" );
				geomArgs = new boolean[] { true, true };
				break;
			case SpatialAnalysis.SYMDIFFERENCE:
				ogcFunction.append( "OGC_SYMMETRICDIFFERENCE" );
				geomArgs = new boolean[] { true, true };
				break;
			case SpatialAnalysis.UNION:
				ogcFunction.append( "OGC_UNION" );
				geomArgs = new boolean[] { true, true };
				break;
			default:
				throw new IllegalArgumentException(
						"Unknown SpatialAnalysisFunction ("
								+ spatialAnalysisFunction + ")."
				);
		}

		if ( args.size() < geomArgs.length ) {
			throw new QueryException(
					"Insufficient arguments for spatial analysis function (function type:  "
							+ spatialAnalysisFunction + ")."
			);
		}

		ogcFunction.append( "(" );
		for ( int i = 0; i < geomArgs.length; i++ ) {
			if ( i > 0 ) {
				ogcFunction.append( "," );
			}
			if ( geomArgs[i] ) {
				wrapInSTGeometry( (String) args.get( i ), ogcFunction );
			}
			else {
				ogcFunction.append( args.get( i ) );
			}
		}
		ogcFunction.append( ")" );
		if ( isGeomReturn ) {
			ogcFunction.append( ".geom" );
		}
		return ogcFunction.toString();
	}

	private static StringBuffer wrapInSTGeometry(String geomColumn, StringBuffer toAdd) {
		return toAdd.append( "MDSYS.ST_GEOMETRY(" ).append( geomColumn )
				.append( ")" );
	}

	static String getNativeSpatialAnalysisSQL(List args, int spatialAnalysis) {
		return getOGCSpatialAnalysisSQL( args, spatialAnalysis );
	}

	static String getSpatialAnalysisSQL(List args, int spatialAnalysisFunction) {
		return getOGCSpatialAnalysisSQL( args, spatialAnalysisFunction );
	}

	/**
	 * Implementation of the OGC astext function for HQL.
	 */
	private static class AsTextFunction extends StandardSQLFunction {

		private AsTextFunction() {
			super( "astext", StandardBasicTypes.STRING );
		}

		public String render(Type firstArgumentType, final List args, final SessionFactoryImplementor factory) {
			final StringBuilder buf = new StringBuilder();
			if ( args.isEmpty() ) {
				throw new IllegalArgumentException( "First Argument in arglist must be object " + "to which method is applied" );
			}
			buf.append( "TO_CHAR(SDO_UTIL.TO_WKTGEOMETRY(" ).append( args.get( 0 ) ).append( "))" );
			return buf.toString();
		}
	}

	/**
	 * HQL Spatial relation function.
	 */
	private static class SpatialRelateFunction extends StandardSQLFunction {
		private final int relation;
		private final boolean isOGCStrict;
		private OracleSDOSupport sdo;

		private SpatialRelateFunction(
				final String name,
				final int relation,
				final boolean isOGCStrict,
				OracleSDOSupport sdo) {
			super( name, StandardBasicTypes.BOOLEAN );
			this.relation = relation;
			this.isOGCStrict = isOGCStrict;
			this.sdo = sdo;
		}

		public String render(Type firstArgumentType, final List args, final SessionFactoryImplementor factory) {

			if ( args.size() < 2 ) {
				throw new QueryException(
						"Spatial relate functions require at least two arguments"
				);
			}

			return isOGCStrict ?
					sdo.getOGCSpatialRelateSQL(
							(String) args.get( 0 ),
							(String) args.get( 1 ), this.relation
					) :
					sdo.getNativeSpatialRelateSQL(
							(String) args.get( 0 ),
							(String) args.get( 1 ), this.relation
					);
		}

	}


	private static class SpatialAnalysisFunction extends StandardSQLFunction {
		private final int analysis;
		private final boolean isOGCStrict;

		private SpatialAnalysisFunction(String name, Type returnType, int analysis, boolean isOGCStrict) {
			super( name, returnType );
			this.analysis = analysis;
			this.isOGCStrict = isOGCStrict;
		}

		private SpatialAnalysisFunction(String name, int analysis, boolean isOGCStrict) {
			this( name, null, analysis, isOGCStrict );
		}

		public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) {
			return isOGCStrict ? getSpatialAnalysisSQL( args, this.analysis ) : getNativeSpatialAnalysisSQL(
					args,
					analysis
			);
		}

	}

	static class SpatialAggregationFunction extends StandardSQLFunction {

		private final int aggregation;
		private final OracleSDOSupport sdo;

		private SpatialAggregationFunction(String name, int aggregation, OracleSDOSupport dialect) {
			super( name );
			this.aggregation = aggregation;
			this.sdo = dialect;
		}

		public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) {
			return sdo.getSpatialAggregateSQL(
					(String) args.get( 0 ),
					this.aggregation
			);
		}
	}
}
