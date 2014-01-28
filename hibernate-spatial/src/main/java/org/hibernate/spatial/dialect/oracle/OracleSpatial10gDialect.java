/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2014 Geovise BVBA
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
package org.hibernate.spatial.dialect.oracle;

import java.io.Serializable;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.GeometrySqlTypeDescriptor;
import org.hibernate.spatial.GeometryType;
import org.hibernate.spatial.HibernateSpatialConfiguration;
import org.hibernate.spatial.SpatialAnalysis;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.spatial.dialect.oracle.criterion.OracleSpatialAggregate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Spatial Dialect for Oracle10g databases.
 *
 * @author Karel Maesen
 */
public class OracleSpatial10gDialect extends Oracle10gDialect implements
		SpatialDialect, Serializable {


	/**
	 * Implementation of the OGC astext function for HQL.
	 */
	private class AsTextFunction extends StandardSQLFunction {

		private AsTextFunction() {
			super("astext", StandardBasicTypes.STRING);
		}

		public String render(Type firstArgumentType, final List args,
							 final SessionFactoryImplementor factory) {

			StringBuffer buf = new StringBuffer();
			if (args.isEmpty()) {
				throw new IllegalArgumentException(
						"First Argument in arglist must be object "
								+ "to which method is applied");
			}

			buf.append("TO_CHAR(SDO_UTIL.TO_WKTGEOMETRY(").append(args.get(0))
					.append("))");
			return buf.toString();
		}
	}


	/**
	 * HQL Spatial relation function.
	 */
	private class SpatialRelateFunction extends StandardSQLFunction {
		private final int relation;

		private SpatialRelateFunction(final String name, final int relation) {
			super(name, isOGCStrict() ? StandardBasicTypes.BOOLEAN : SDOBooleanType.INSTANCE);
			this.relation = relation;
		}

		public String render(Type firstArgumentType, final List args,
							 final SessionFactoryImplementor factory) {

			if ( args.size() < 2 ) {
				throw new QueryException(
						"Spatial relate functions require at least two arguments"
				);
			}

			return isOGCStrict() ? getOGCSpatialRelateSQL(
					(String) args.get( 0 ),
					(String) args.get( 1 ),
					this.relation
			) : getNativeSpatialRelateSQL( (String) args.get( 0 ), (String) args.get( 1 ), this.relation );
		}

	}

	private class SpatialAnalysisFunction extends StandardSQLFunction {
		private final int analysis;

		private SpatialAnalysisFunction(String name, Type returnType,
										int analysis) {
			super(name, returnType);
			this.analysis = analysis;
		}

		public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) {
			return isOGCStrict() ? getSpatialAnalysisSQL(args, this.analysis,
					false) : getNativeSpatialAnalysisSQL(args, analysis);
		}

	}

	private class SpatialAggregationFunction extends StandardSQLFunction {

		private final int aggregation;

		private SpatialAggregationFunction(String name, Type returnType,
										   boolean isProjection, int aggregation) {
			super(name, returnType);
			this.aggregation = aggregation;
		}

		public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) {
			return getNativeSpatialAggregateSQL((String) args.get(0),
					this.aggregation);
		}
	}

	public final static String SHORT_NAME = "oraclespatial";

	private final boolean  isOgcStrict;

	private final ConnectionFinder connectionFinder;

	public OracleSpatial10gDialect() {
		this(new HibernateSpatialConfiguration());
	}
		
	public OracleSpatial10gDialect(HibernateSpatialConfiguration config) {
		super();
		this.isOgcStrict = config.isOgcStrictMode();
		ConnectionFinder finder = config.getConnectionFinder();
		this.connectionFinder = finder == null ? new DefaultConnectionFinder() : finder;

		// register geometry type
		registerColumnType(java.sql.Types.STRUCT, "MDSYS.SDO_GEOMETRY");

		// registering OGC functions
		// (spec_simplefeatures_sql_99-04.pdf)

		// section 2.1.1.1
		registerFunction("dimension", new GetDimensionFunction());
		registerFunction("geometrytype", new GetGeometryTypeFunction());
		registerFunction("srid", new SDOObjectProperty("SDO_SRID",
				StandardBasicTypes.INTEGER));
		registerFunction("envelope",
				new StandardSQLFunction("SDO_GEOM.SDO_MBR", GeometryType.INSTANCE));
		registerFunction("astext", new AsTextFunction());

		registerFunction("asbinary", new StandardSQLFunction(
				"SDO_UTIL.TO_WKBGEOMETRY", StandardBasicTypes.BINARY));
		registerFunction("isempty", new WrappedOGCFunction("OGC_ISEMPTY",
				StandardBasicTypes.BOOLEAN, new boolean[]{true}));
		registerFunction("issimple", new WrappedOGCFunction("OGC_ISSIMPLE",
				StandardBasicTypes.BOOLEAN, new boolean[]{true}));
		registerFunction("boundary", new WrappedOGCFunction("OGC_BOUNDARY",
				GeometryType.INSTANCE,
				new boolean[]{true}));

		// registerFunction("area", new AreaFunction());

		// Register functions for spatial relation constructs
		// section 2.1.1.2
		registerFunction("overlaps", new SpatialRelateFunction("overlaps",
				SpatialRelation.OVERLAPS));
		registerFunction("intersects", new SpatialRelateFunction("intersects",
				SpatialRelation.INTERSECTS));
		registerFunction("contains", new SpatialRelateFunction("contains",
				SpatialRelation.CONTAINS));
		registerFunction("crosses", new SpatialRelateFunction("crosses",
				SpatialRelation.CROSSES));
		registerFunction("disjoint", new SpatialRelateFunction("disjoint",
				SpatialRelation.DISJOINT));
		registerFunction("equals", new SpatialRelateFunction("equals",
				SpatialRelation.EQUALS));
		registerFunction("touches", new SpatialRelateFunction("touches",
				SpatialRelation.TOUCHES));
		registerFunction("within", new SpatialRelateFunction("within",
				SpatialRelation.WITHIN));
		registerFunction("relate", new WrappedOGCFunction("OGC_RELATE",
				StandardBasicTypes.BOOLEAN, new boolean[]{true, true, false}));

		// Register spatial analysis functions.
		// Section 2.1.1.3
		registerFunction("distance", new SpatialAnalysisFunction("distance",
				StandardBasicTypes.DOUBLE, SpatialAnalysis.DISTANCE));
		registerFunction("buffer", new SpatialAnalysisFunction("buffer",
				GeometryType.INSTANCE,
				SpatialAnalysis.BUFFER));
		registerFunction("convexhull", new SpatialAnalysisFunction(
				"convexhull", GeometryType.INSTANCE,
				SpatialAnalysis.CONVEXHULL));
		registerFunction("difference", new SpatialAnalysisFunction(
				"difference", GeometryType.INSTANCE,
				SpatialAnalysis.DIFFERENCE));
		registerFunction("intersection", new SpatialAnalysisFunction(
				"intersection", GeometryType.INSTANCE,
				SpatialAnalysis.INTERSECTION));
		registerFunction("symdifference", new SpatialAnalysisFunction(
				"symdifference", GeometryType.INSTANCE,
				SpatialAnalysis.SYMDIFFERENCE));
		registerFunction("geomunion", new SpatialAnalysisFunction("union",
				GeometryType.INSTANCE,
				SpatialAnalysis.UNION));
		// we rename OGC union to geomunion because union is a reserved SQL
		// keyword. (See also postgis documentation).

		// portable spatial aggregate functions
		registerFunction("extent", new SpatialAggregationFunction("extent",
				GeometryType.INSTANCE, false,
				OracleSpatialAggregate.EXTENT));

		//other common functions
		registerFunction("transform", new StandardSQLFunction("SDO_CS.TRANSFORM",
				GeometryType.INSTANCE));
		registerFunction("dwithin", new StandardSQLFunction("SDO_WITHIN_DISTANCE" , SDOBooleanType.INSTANCE));

		// Oracle specific Aggregate functions
		registerFunction("centroid", new SpatialAggregationFunction("extent",
				GeometryType.INSTANCE, false,
				OracleSpatialAggregate.CENTROID));

		registerFunction("concat_lines", new SpatialAggregationFunction(
				"extent", GeometryType.INSTANCE, false,
				OracleSpatialAggregate.CONCAT_LINES));

		registerFunction("aggr_convexhull", new SpatialAggregationFunction(
				"extent", GeometryType.INSTANCE, false,
				OracleSpatialAggregate.CONVEXHULL));

		registerFunction("aggr_union", new SpatialAggregationFunction("extent",
				GeometryType.INSTANCE, false,
				OracleSpatialAggregate.UNION));

		registerFunction("lrs_concat", new SpatialAggregationFunction(
				"lrsconcat", GeometryType.INSTANCE,
				false, OracleSpatialAggregate.LRS_CONCAT));
	}

	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		if (code == 3000) return "SDO_GEOMETRY";
		return super.getTypeName( code, length, precision, scale );
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		if (sqlTypeDescriptor instanceof GeometrySqlTypeDescriptor) {
			return new SDOGeometryTypeDescriptor(new OracleJDBCTypeFactory(this.connectionFinder));
		}
		return super.remapSqlTypeDescriptor( sqlTypeDescriptor );
	}

	public String getNativeSpatialRelateSQL(String arg1, String arg2,
											int spatialRelation) {
		String mask = "";
		boolean negate = false;
		switch (spatialRelation) {
			case SpatialRelation.INTERSECTS:
				mask = "ANYINTERACT"; // OGC Compliance verified
				break;
			case SpatialRelation.CONTAINS:
				mask = "CONTAINS+COVERS";
				break;
			case SpatialRelation.CROSSES:
				throw new UnsupportedOperationException(
						"Oracle Spatial does't have equivalent CROSSES relationship");
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
								+ ")");
		}
		StringBuffer buffer = new StringBuffer();
		if ( negate ) {
			buffer.append( "CASE " );
		}
		buffer.append( "SDO_RELATE(" ).append( arg1 )
				.append( "," )
				.append( arg2 )
				.append( ",'mask=" + mask + "') " );
		if(negate){
			buffer.append( " WHEN 'TRUE' THEN 'FALSE' ELSE 'TRUE' END" );
		}
		return buffer.toString();
	}

	public String getOGCSpatialRelateSQL(String arg1, String arg2,
										 int spatialRelation) {

		StringBuffer ogcFunction = new StringBuffer("MDSYS.");
		switch (spatialRelation) {
			case SpatialRelation.INTERSECTS:
				ogcFunction.append("OGC_INTERSECTS");
				break;
			case SpatialRelation.CONTAINS:
				ogcFunction.append("OGC_CONTAINS");
				break;
			case SpatialRelation.CROSSES:
				ogcFunction.append("OGC_CROSS");
				break;
			case SpatialRelation.DISJOINT:
				ogcFunction.append("OGC_DISJOINT");
				break;
			case SpatialRelation.EQUALS:
				ogcFunction.append("OGC_EQUALS");
				break;
			case SpatialRelation.OVERLAPS:
				ogcFunction.append("OGC_OVERLAP");
				break;
			case SpatialRelation.TOUCHES:
				ogcFunction.append("OGC_TOUCH");
				break;
			case SpatialRelation.WITHIN:
				ogcFunction.append("OGC_WITHIN");
				break;
			default:
				throw new IllegalArgumentException("Unknown SpatialRelation ("
						+ spatialRelation + ").");
		}
		ogcFunction.append("(").append("MDSYS.ST_GEOMETRY.FROM_SDO_GEOM(")
				.append(arg1).append("),").append(
				"MDSYS.ST_GEOMETRY.FROM_SDO_GEOM(").append(arg2)
				.append(")").append(")");
		return ogcFunction.toString();

	}

	public String getNativeSpatialAggregateSQL(String arg1, int aggregation) {

		StringBuffer aggregateFunction = new StringBuffer();

		SpatialAggregate sa = new SpatialAggregate(aggregation);

		if (sa._aggregateSyntax == null) {
			throw new IllegalArgumentException("Unknown Spatial Aggregation ("
					+ aggregation + ").");
		}

		aggregateFunction.append(sa._aggregateSyntax);

		aggregateFunction.append("(");
		if (sa.isAggregateType()) {
			aggregateFunction.append("SDOAGGRTYPE(");
		}
		aggregateFunction.append(arg1);
		// TODO tolerance must by configurable
		if (sa.isAggregateType()) {
			aggregateFunction.append(", ").append(.001).append(")");
		}
		aggregateFunction.append(")");

		return aggregateFunction.toString();
	}

	private StringBuffer wrapInSTGeometry(String geomColumn, StringBuffer toAdd) {
		return toAdd.append("MDSYS.ST_GEOMETRY(").append(geomColumn)
				.append( ")" );
	}

	public String getSpatialFilterExpression(String columnName) {
		StringBuffer buffer = new StringBuffer("SDO_FILTER(");
		buffer.append(columnName);
		buffer.append(",?) = 'TRUE' ");
		return buffer.toString();
	}

	public String getSpatialRelateSQL(String columnName, int spatialRelation) {

		String sql = ( isOGCStrict() ?
				getOGCSpatialRelateSQL(columnName,"?",spatialRelation) + " = 1" :
				 getNativeSpatialRelateSQL( columnName, "?", spatialRelation )  + " = 'TRUE'" );
		sql += " and " + columnName + " is not null";
		return sql;
	}

	public String getSpatialAnalysisSQL(List args, int spatialAnalysisFunction,
										boolean useFilter) {
		return isOGCStrict() ? getOGCSpatialAnalysisSQL(args,
				spatialAnalysisFunction) : getNativeSpatialAnalysisSQL(args,
				spatialAnalysisFunction);
	}

	public String getSpatialAggregateSQL(String columnName,
										 int spatialAggregateFunction) {
		return getNativeSpatialAggregateSQL(
				columnName,
				spatialAggregateFunction
		);
	}

	public String getDWithinSQL(String columnName) {
		return "SDO_WITHIN_DISTANCE (" + columnName + ",?, ?) = 'TRUE' ";
	}

	public String getHavingSridSQL(String columnName) {
		return String.format(" (MDSYS.ST_GEOMETRY(%s).ST_SRID() = ?)", columnName);
	}

	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		return String.format(
				"( MDSYS.ST_GEOMETRY(%s).ST_ISEMPTY() = %d )",
				columnName,
				isEmpty ? 1 : 0
		);
	}

	private String getOGCSpatialAnalysisSQL(List args,
											int spatialAnalysisFunction) {
		boolean[] geomArgs;
		StringBuffer ogcFunction = new StringBuffer("MDSYS.");
		boolean isGeomReturn = true;
		switch (spatialAnalysisFunction) {
			case SpatialAnalysis.BUFFER:
				ogcFunction.append("OGC_BUFFER");
				geomArgs = new boolean[]{true, false};
				break;
			case SpatialAnalysis.CONVEXHULL:
				ogcFunction.append("OGC_CONVEXHULL");
				geomArgs = new boolean[]{true};
				break;
			case SpatialAnalysis.DIFFERENCE:
				ogcFunction.append("OGC_DIFFERENCE");
				geomArgs = new boolean[]{true, true};
				break;
			case SpatialAnalysis.DISTANCE:
				ogcFunction.append("OGC_DISTANCE");
				geomArgs = new boolean[]{true, true};
				isGeomReturn = false;
				break;
			case SpatialAnalysis.INTERSECTION:
				ogcFunction.append("OGC_INTERSECTION");
				geomArgs = new boolean[]{true, true};
				break;
			case SpatialAnalysis.SYMDIFFERENCE:
				ogcFunction.append("OGC_SYMMETRICDIFFERENCE");
				geomArgs = new boolean[]{true, true};
				break;
			case SpatialAnalysis.UNION:
				ogcFunction.append("OGC_UNION");
				geomArgs = new boolean[]{true, true};
				break;
			default:
				throw new IllegalArgumentException(
						"Unknown SpatialAnalysisFunction ("
								+ spatialAnalysisFunction + ").");
		}

		if (args.size() < geomArgs.length)
			throw new QueryException(
					"Insufficient arguments for spatial analysis function (function type:  "
							+ spatialAnalysisFunction + ").");

		ogcFunction.append("(");
		for (int i = 0; i < geomArgs.length; i++) {
			if (i > 0)
				ogcFunction.append(",");
			if (geomArgs[i])
				wrapInSTGeometry((String) args.get(i), ogcFunction);
			else
				ogcFunction.append(args.get(i));
		}
		ogcFunction.append(")");
		if (isGeomReturn)
			ogcFunction.append(".geom");
		return ogcFunction.toString();
	}

	private String getNativeSpatialAnalysisSQL(List args, int spatialAnalysis) {
		return getOGCSpatialAnalysisSQL( args, spatialAnalysis );
	}

	/**
	 * Reports whether this dialect is in OGC_STRICT mode or not.
	 *
	 * This method is for testing purposes.
	 * @return true if in OGC_STRICT mode, false otherwise
	 *
	 */
	public boolean isOGCStrict() {
		return isOgcStrict;
	}


	/**
	 * Reports the ConnectionFinder used by this Dialect (or rather its associated TypeDescriptor).
	 *
	 * This method is mainly used for testing purposes.
	 * @return the ConnectionFinder in use
	 */
	public ConnectionFinder getConnectionFinder(){
		return connectionFinder;
	}

	public boolean supportsFiltering() {
		return true;
	}

	public boolean supports(SpatialFunction function) {
		//exception for crosses -- not supported when not OGC_STRICT
		if (!isOGCStrict() && SpatialFunction.crosses.equals(function) ) return false;
		return (getFunctions().get(function.toString()) != null);
	}


}
