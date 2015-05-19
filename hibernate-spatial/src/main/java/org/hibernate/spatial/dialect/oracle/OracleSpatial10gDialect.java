/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;


import java.io.Serializable;
import java.sql.Types;
import java.util.List;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;
import org.geolatte.geom.codec.db.oracle.DefaultConnectionFinder;
import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;

import org.hibernate.QueryException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.HibernateSpatialConfiguration;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialAnalysis;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.spatial.dialect.oracle.criterion.OracleSpatialAggregate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Spatial Dialect for Oracle10g databases.
 *
 * @author Karel Maesen
 */
public class OracleSpatial10gDialect extends Oracle10gDialect implements SpatialDialect, Serializable {

	private final boolean isOgcStrict;
	private final ConnectionFinder connectionFinder;


	/**
	 * Constructs the dialect with a default configuration
	 */
	public OracleSpatial10gDialect() {
		this( new HibernateSpatialConfiguration() );
	}

	/**
	 * Constructs the dialect with the specified configuration
	 *
	 * @param config the {@code HibernateSpatialConfiguration} that configures this dialect.
	 */
	public OracleSpatial10gDialect(HibernateSpatialConfiguration config) {
		super();
		this.isOgcStrict = config.isOgcStrictMode();
		final ConnectionFinder finder = config.getConnectionFinder();
		this.connectionFinder = finder == null ? new DefaultConnectionFinder() : finder;


		// register geometry type
		registerColumnType( Types.STRUCT, "MDSYS.SDO_GEOMETRY" );

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		registerFunction( "dimension", new GetDimensionFunction() );
		registerFunction( "geometrytype", new GetGeometryTypeFunction() );
		registerFunction( "srid", new SDOObjectProperty( "SDO_SRID", StandardBasicTypes.INTEGER ) );
		registerFunction( "envelope", new StandardSQLFunction( "SDO_GEOM.SDO_MBR" ) );
		registerFunction( "astext", new AsTextFunction() );
		registerFunction( "asbinary", new StandardSQLFunction( "SDO_UTIL.TO_WKBGEOMETRY", StandardBasicTypes.BINARY ) );
		registerFunction(
				"isempty",
				new WrappedOGCFunction( "OGC_ISEMPTY", StandardBasicTypes.BOOLEAN, new boolean[] {true} )
		);
		registerFunction(
				"issimple",
				new WrappedOGCFunction( "OGC_ISSIMPLE", StandardBasicTypes.BOOLEAN, new boolean[] {true} )
		);
		registerFunction( "boundary", new WrappedOGCFunction( "OGC_BOUNDARY", new boolean[] {true} ) );

		// registerFunction("area", new AreaFunction());

		// Register functions for spatial relation constructs
		// section 2.1.1.2
		registerFunction( "overlaps", new SpatialRelateFunction( "overlaps", SpatialRelation.OVERLAPS ) );
		registerFunction( "intersects", new SpatialRelateFunction( "intersects", SpatialRelation.INTERSECTS ) );
		registerFunction( "contains", new SpatialRelateFunction( "contains", SpatialRelation.CONTAINS ) );
		registerFunction( "crosses", new SpatialRelateFunction( "crosses", SpatialRelation.CROSSES ) );
		registerFunction( "disjoint", new SpatialRelateFunction( "disjoint", SpatialRelation.DISJOINT ) );
		registerFunction( "equals", new SpatialRelateFunction( "equals", SpatialRelation.EQUALS ) );
		registerFunction( "touches", new SpatialRelateFunction( "touches", SpatialRelation.TOUCHES ) );
		registerFunction( "within", new SpatialRelateFunction( "within", SpatialRelation.WITHIN ) );
		registerFunction(
				"relate",
				new WrappedOGCFunction( "OGC_RELATE", StandardBasicTypes.BOOLEAN, new boolean[] {true, true, false} )
		);

		// Register spatial analysis functions.
		// Section 2.1.1.3
		registerFunction(
				"distance",
				new SpatialAnalysisFunction( "distance", StandardBasicTypes.DOUBLE, SpatialAnalysis.DISTANCE )
		);
		registerFunction( "buffer", new SpatialAnalysisFunction( "buffer", SpatialAnalysis.BUFFER ) );
		registerFunction( "convexhull", new SpatialAnalysisFunction( "convexhull", SpatialAnalysis.CONVEXHULL ) );
		registerFunction( "difference", new SpatialAnalysisFunction( "difference", SpatialAnalysis.DIFFERENCE ) );
		registerFunction( "intersection", new SpatialAnalysisFunction( "intersection", SpatialAnalysis.INTERSECTION ) );
		registerFunction(
				"symdifference",
				new SpatialAnalysisFunction( "symdifference", SpatialAnalysis.SYMDIFFERENCE )
		);
		registerFunction( "geomunion", new SpatialAnalysisFunction( "union", SpatialAnalysis.UNION ) );
		// we rename OGC union to geomunion because union is a reserved SQL
		// keyword. (See also postgis documentation).

		// portable spatial aggregate functions
		registerFunction( "extent", new SpatialAggregationFunction( "extent", false, OracleSpatialAggregate.EXTENT ) );

		//other common functions

		registerFunction( "transform", new StandardSQLFunction( "SDO_CS.TRANSFORM" ) );

		// Oracle specific Aggregate functions
		registerFunction(
				"centroid",
				new SpatialAggregationFunction( "extent", false, OracleSpatialAggregate.CENTROID )
		);
		registerFunction(
				"concat_lines",
				new SpatialAggregationFunction( "extent", false, OracleSpatialAggregate.CONCAT_LINES )
		);
		registerFunction(
				"aggr_convexhull",
				new SpatialAggregationFunction( "extent", false, OracleSpatialAggregate.CONVEXHULL )
		);
		registerFunction(
				"aggr_union",
				new SpatialAggregationFunction( "extent", false, OracleSpatialAggregate.UNION )
		);
		registerFunction(
				"lrs_concat",
				new SpatialAggregationFunction( "lrsconcat", false, OracleSpatialAggregate.LRS_CONCAT )
		);
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);

		final SDOGeometryTypeDescriptor sdoGeometryTypeDescriptor = new SDOGeometryTypeDescriptor(
				new OracleJDBCTypeFactory(
						this.connectionFinder
				)
		);

		typeContributions.contributeType( new GeolatteGeometryType( sdoGeometryTypeDescriptor ) );
		typeContributions.contributeType( new JTSGeometryType( sdoGeometryTypeDescriptor ) );

	}

	String getNativeSpatialRelateSQL(String arg1, String arg2, int spatialRelation) {
		String mask = "";
		boolean negate = false;
		switch ( spatialRelation ) {
			case SpatialRelation.INTERSECTS:
				mask = "ANYINTERACT";
				break;
			case SpatialRelation.CONTAINS:
				mask = "CONTAINS+COVERS";
				break;
			case SpatialRelation.CROSSES:
				throw new UnsupportedOperationException(
						"Oracle Spatial does't have equivalent CROSSES relationship"
				);
			case SpatialRelation.DISJOINT:
				mask = "ANYINTERACT";
				negate = true;
				break;
			case SpatialRelation.EQUALS:
				mask = "EQUAL";
				break;
			case SpatialRelation.OVERLAPS:
				mask = "OVERLAPBDYDISJOINT+OVERLAPBDYINTERSECT";
				break;
			case SpatialRelation.TOUCHES:
				mask = "TOUCH";
				break;
			case SpatialRelation.WITHIN:
				mask = "INSIDE+COVEREDBY";
				break;
			default:
				throw new IllegalArgumentException(
						"undefined SpatialRelation passed (" + spatialRelation
								+ ")"
				);
		}
		final StringBuffer buffer = new StringBuffer( "CASE SDO_RELATE(" ).append( arg1 )
				.append( "," )
				.append( arg2 )
				.append( ",'mask=" + mask + "') " );
		if ( !negate ) {
			buffer.append( " WHEN 'TRUE' THEN 1 ELSE 0 END" );
		}
		else {
			buffer.append( " WHEN 'TRUE' THEN 0 ELSE 1 END" );
		}
		return buffer.toString();
	}

	String getOGCSpatialRelateSQL(String arg1, String arg2, int spatialRelation) {
		final StringBuffer ogcFunction = new StringBuffer( "MDSYS." );
		switch ( spatialRelation ) {
			case SpatialRelation.INTERSECTS:
				ogcFunction.append( "OGC_INTERSECTS" );
				break;
			case SpatialRelation.CONTAINS:
				ogcFunction.append( "OGC_CONTAINS" );
				break;
			case SpatialRelation.CROSSES:
				ogcFunction.append( "OGC_CROSS" );
				break;
			case SpatialRelation.DISJOINT:
				ogcFunction.append( "OGC_DISJOINT" );
				break;
			case SpatialRelation.EQUALS:
				ogcFunction.append( "OGC_EQUALS" );
				break;
			case SpatialRelation.OVERLAPS:
				ogcFunction.append( "OGC_OVERLAP" );
				break;
			case SpatialRelation.TOUCHES:
				ogcFunction.append( "OGC_TOUCH" );
				break;
			case SpatialRelation.WITHIN:
				ogcFunction.append( "OGC_WITHIN" );
				break;
			default:
				throw new IllegalArgumentException(
						"Unknown SpatialRelation ("
								+ spatialRelation + ")."
				);
		}
		ogcFunction.append( "(" ).append( "MDSYS.ST_GEOMETRY.FROM_SDO_GEOM(" )
				.append( arg1 ).append( ")," ).append(
				"MDSYS.ST_GEOMETRY.FROM_SDO_GEOM("
		).append( arg2 )
				.append( ")" ).append( ")" );
		return ogcFunction.toString();

	}

	String getNativeSpatialAggregateSQL(String arg1, int aggregation) {
		final StringBuffer aggregateFunction = new StringBuffer();
		final SpatialAggregate sa = new SpatialAggregate( aggregation );

		if ( sa.getAggregateSyntax() == null ) {
			throw new IllegalArgumentException(
					"Unknown Spatial Aggregation ("
							+ aggregation + ")."
			);
		}

		aggregateFunction.append( sa.getAggregateSyntax() );

		aggregateFunction.append( "(" );
		if ( sa.isAggregateType() ) {
			aggregateFunction.append( "SDOAGGRTYPE(" );
		}
		aggregateFunction.append( arg1 );
		// TODO tolerance must by configurable
		if ( sa.isAggregateType() ) {
			aggregateFunction.append( ", " ).append( .001 ).append( ")" );
		}
		aggregateFunction.append( ")" );

		return aggregateFunction.toString();
	}

	private StringBuffer wrapInSTGeometry(String geomColumn, StringBuffer toAdd) {
		return toAdd.append( "MDSYS.ST_GEOMETRY(" ).append( geomColumn )
				.append( ")" );
	}

	@Override
	public String getSpatialFilterExpression(String columnName) {
		final StringBuffer buffer = new StringBuffer( "SDO_FILTER(" );
		buffer.append( columnName );
		buffer.append( ",?) = 'TRUE' " );
		return buffer.toString();
	}

	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		String sql = (isOGCStrict() ?
				getOGCSpatialRelateSQL( columnName, "?", spatialRelation ) :
				getNativeSpatialRelateSQL( columnName, "?", spatialRelation )) + " = 1";
		sql += " and " + columnName + " is not null";
		return sql;
	}

	String getSpatialAnalysisSQL(List args, int spatialAnalysisFunction, boolean useFilter) {
		return isOGCStrict() ? getOGCSpatialAnalysisSQL( args, spatialAnalysisFunction ) : getNativeSpatialAnalysisSQL(
				args,
				spatialAnalysisFunction
		);
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int spatialAggregateFunction) {
		return getNativeSpatialAggregateSQL( columnName, spatialAggregateFunction );
	}

	@Override
	public String getDWithinSQL(String columnName) {
		return "SDO_WITHIN_DISTANCE (" + columnName + ",?, ?) = 'TRUE' ";
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return String.format( " (MDSYS.ST_GEOMETRY(%s).ST_SRID() = ?)", columnName );
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		return String.format( "( MDSYS.ST_GEOMETRY(%s).ST_ISEMPTY() = %d )", columnName, isEmpty ? 1 : 0 );
	}

	private String getOGCSpatialAnalysisSQL(List args, int spatialAnalysisFunction) {
		boolean[] geomArgs;
		final StringBuffer ogcFunction = new StringBuffer( "MDSYS." );
		boolean isGeomReturn = true;
		switch ( spatialAnalysisFunction ) {
			case SpatialAnalysis.BUFFER:
				ogcFunction.append( "OGC_BUFFER" );
				geomArgs = new boolean[] {true, false};
				break;
			case SpatialAnalysis.CONVEXHULL:
				ogcFunction.append( "OGC_CONVEXHULL" );
				geomArgs = new boolean[] {true};
				break;
			case SpatialAnalysis.DIFFERENCE:
				ogcFunction.append( "OGC_DIFFERENCE" );
				geomArgs = new boolean[] {true, true};
				break;
			case SpatialAnalysis.DISTANCE:
				ogcFunction.append( "OGC_DISTANCE" );
				geomArgs = new boolean[] {true, true};
				isGeomReturn = false;
				break;
			case SpatialAnalysis.INTERSECTION:
				ogcFunction.append( "OGC_INTERSECTION" );
				geomArgs = new boolean[] {true, true};
				break;
			case SpatialAnalysis.SYMDIFFERENCE:
				ogcFunction.append( "OGC_SYMMETRICDIFFERENCE" );
				geomArgs = new boolean[] {true, true};
				break;
			case SpatialAnalysis.UNION:
				ogcFunction.append( "OGC_UNION" );
				geomArgs = new boolean[] {true, true};
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

	private String getNativeSpatialAnalysisSQL(List args, int spatialAnalysis) {
		return getOGCSpatialAnalysisSQL( args, spatialAnalysis );
	}

	/**
	 * Reports whether this dialect is in OGC_STRICT mode or not.
	 * <p/>
	 * This method is for testing purposes.
	 *
	 * @return true if in OGC_STRICT mode, false otherwise
	 */
	public boolean isOGCStrict() {
		return isOgcStrict;
	}

	/**
	 * Reports the ConnectionFinder used by this Dialect (or rather its associated TypeDescriptor).
	 * <p/>
	 * This method is mainly used for testing purposes.
	 *
	 * @return the ConnectionFinder in use
	 */
	public ConnectionFinder getConnectionFinder() {
		return connectionFinder;
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return (getFunctions().get( function.toString() ) != null);
	}

	/**
	 * Implementation of the OGC astext function for HQL.
	 */
	private static class AsTextFunction extends StandardSQLFunction {

		private AsTextFunction() {
			super( "astext", StandardBasicTypes.STRING );
		}

		public String render(Type firstArgumentType, final List args, final SessionFactoryImplementor factory) {
			final StringBuffer buf = new StringBuffer();
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
	private class SpatialRelateFunction extends StandardSQLFunction {
		private final int relation;

		private SpatialRelateFunction(final String name, final int relation) {
			super(
					name, isOGCStrict() ? StandardBasicTypes.BOOLEAN
							: new SDOBooleanType()
			);
			this.relation = relation;
		}

		public String render(Type firstArgumentType, final List args, final SessionFactoryImplementor factory) {

			if ( args.size() < 2 ) {
				throw new QueryException(
						"Spatial relate functions require at least two arguments"
				);
			}

			return isOGCStrict() ?
					getOGCSpatialRelateSQL(
							(String) args.get( 0 ),
							(String) args.get( 1 ), this.relation
					) :
					getNativeSpatialRelateSQL(
							(String) args.get( 0 ),
							(String) args.get( 1 ), this.relation
					);
		}

	}

	private class SpatialAnalysisFunction extends StandardSQLFunction {
		private final int analysis;

		private SpatialAnalysisFunction(String name, Type returnType, int analysis) {
			super( name, returnType );
			this.analysis = analysis;
		}

		private SpatialAnalysisFunction(String name, int analysis) {
			this( name, null, analysis );
		}

		public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) {
			return isOGCStrict() ? getSpatialAnalysisSQL(
					args, this.analysis,
					false
			) : getNativeSpatialAnalysisSQL( args, analysis );
		}

	}

	private class SpatialAggregationFunction extends StandardSQLFunction {

		private final int aggregation;

		private SpatialAggregationFunction(String name, boolean isProjection, int aggregation) {
			super( name );
			this.aggregation = aggregation;
		}

		public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) {
			return getNativeSpatialAggregateSQL(
					(String) args.get( 0 ),
					this.aggregation
			);
		}
	}


}
