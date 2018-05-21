/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.db2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.HibernateSpatialConfigurationSettings;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialAggregate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import static java.lang.String.format;

/**
 * @author David Adler, Adtech Geospatial
 * creation-date: 5/22/2014
 */
public class DB2SpatialDialect extends DB2Dialect implements SpatialDialect {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final Map<Integer, String> spatialRelationNames = new HashMap<>();

	/**
	 * Construct a DB2Spatial dialect. Register the geometry type and spatial
	 * functions supported.
	 */
	public DB2SpatialDialect() {
		super();
		registerSpatialType();
		registerSpatialFunctions();
		initializeRelationNames();
	}

	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final DB2GeometryTypeDescriptor typeDescriptor = mkDescriptor( serviceRegistry );
		typeContributions.contributeType( new GeolatteGeometryType( typeDescriptor ) );
		typeContributions.contributeType( new JTSGeometryType( typeDescriptor ) );

		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.INSTANCE );
	}

	private DB2GeometryTypeDescriptor mkDescriptor(ServiceRegistry serviceRegistry) {
		ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		Integer srid = retrieveSridFromConfiguration( configurationService );
		return new DB2GeometryTypeDescriptor( srid );
	}

	private Integer retrieveSridFromConfiguration(ConfigurationService configurationService) {
		Integer srid = 0;
		try {
			srid = Integer.parseInt( configurationService.getSetting(
					HibernateSpatialConfigurationSettings.DB2_DEFAULT_SRID,
					String.class,
					"0"
			) );
		}
		catch (NumberFormatException e) {
			throw new HibernateException(
					"Invalid format for configuration parameter (Integer expected): " + HibernateSpatialConfigurationSettings.DB2_DEFAULT_SRID,
					e
			);
		}
		return srid;
	}

	/**
	 * Set up the map relating Hibernate Spatial relation constants to DB2 function names.
	 */
	private void initializeRelationNames() {

		spatialRelationNames.put( SpatialRelation.EQUALS, "ST_EQUALS" );
		spatialRelationNames.put( SpatialRelation.DISJOINT, "ST_DISJOINT" );
		spatialRelationNames.put( SpatialRelation.TOUCHES, "ST_TOUCHES" );
		spatialRelationNames.put( SpatialRelation.CROSSES, "ST_CROSSES" );
		spatialRelationNames.put( SpatialRelation.WITHIN, "ST_WITHIN" );
		spatialRelationNames.put( SpatialRelation.OVERLAPS, "ST_OVERLAPS" );
		spatialRelationNames.put( SpatialRelation.CONTAINS, "ST_CONTAINS" );
		spatialRelationNames.put( SpatialRelation.INTERSECTS, "ST_INTERSECTS" );
	}


	/**
	 * Register the spatial type.
	 * The type, CLOB or BLOB is defined in DB2GeometryTypeDescriptor and must match
	 * the type specified in the DB2_PROGRAM transform function.
	 */
	private void registerSpatialType() {

		// Register Geometry column type
		registerColumnType( java.sql.Types.CLOB, " db2gse.ST_Geometry" );
	}

	/**
	 * Register the spatial functions supported.
	 */
	private void registerSpatialFunctions() {

		// Register functions used as spatial predicates
		// The first parameter of registerFunction is the name that Hibernate looks for in the HQL.
		// The first parameter of StandardSQLFunction is the DB2 spatial function name that will replace it.
		// The second parameter of StandardSQLFunction is the return type of the function, always integer for functions used as predicates.
		// This is used by Hibernate independent of Hibernate Spatial.
		//
		// Note that this somewhat duplicates the information in spatialRelationNames used by getSpatialRelateSQL which
		// is invoked by Hibernate Spatial to handle SpatialRelateExpression when this is used in a Criteria.

		registerFunction( "equals", new StandardSQLFunction(
				"db2gse.ST_Equals",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "disjoint", new StandardSQLFunction(
				"db2gse.ST_Disjoint",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "touches", new StandardSQLFunction(
				"db2gse.ST_Touches",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "crosses", new StandardSQLFunction(
				"db2gse.ST_Crosses",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );

		registerFunction( "within", new StandardSQLFunction(
				"db2gse.ST_Within",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "overlaps", new StandardSQLFunction(
				"db2gse.ST_Overlaps",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "contains", new StandardSQLFunction(
				"db2gse.ST_Contains",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "intersects", new StandardSQLFunction(
				"db2gse.ST_Intersects",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "relate", new StandardSQLFunction(
				"db2gse.ST_Relate",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );

		// Register functions on Geometry
		registerFunction( "dimension", new StandardSQLFunction(
				"db2gse.ST_Dimension",
				StandardBasicTypes.INTEGER
		) );
		registerFunction( "geometrytype", new StandardSQLFunction(
				"db2gse.ST_GeometryType",
				StandardBasicTypes.STRING
		) );
		registerFunction( "srid", new StandardSQLFunction(
				"db2gse.ST_Srsid",
				StandardBasicTypes.INTEGER
		) );
		registerFunction( "envelope", new StandardSQLFunction(
				"db2gse.ST_Envelope"
		) );
		registerFunction( "astext", new StandardSQLFunction(
				"db2gse.ST_AsText",
				StandardBasicTypes.STRING
		) );
		registerFunction( "asbinary", new StandardSQLFunction(
				"db2gse.ST_AsBinary",
				StandardBasicTypes.BINARY
		) );
		registerFunction( "isempty", new StandardSQLFunction(
				"db2gse.ST_IsEmpty",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "issimple", new StandardSQLFunction(
				"db2gse.ST_IsSimple",
				StandardBasicTypes.NUMERIC_BOOLEAN
		) );
		registerFunction( "boundary", new StandardSQLFunction(
				"db2gse.ST_Boundary"
		) );

		// Register functions that support spatial analysis
		registerFunction( "distance", new StandardSQLFunction(
				"db2gse.ST_Distance",
				StandardBasicTypes.DOUBLE
		) );
		registerFunction( "buffer", new StandardSQLFunction(
				"db2gse.ST_Buffer"
		) );
		registerFunction( "convexhull", new StandardSQLFunction(
				"db2gse.ST_ConvexHull"
		) );
		registerFunction( "intersection", new StandardSQLFunction(
				"db2gse.ST_Intersection"
		) );
		registerFunction( "geomunion", new StandardSQLFunction(
				"db2gse.ST_Union"
		) );
		registerFunction( "difference", new StandardSQLFunction(
				"db2gse.ST_Difference"
		) );
		registerFunction( "symdifference", new StandardSQLFunction(
				"db2gse.ST_SymDifference"
		) );

		// Register non-SFS functions listed in Hibernate Spatial
		registerFunction( "dwithin", new DWithinFunction());

//		// The srid parameter needs to be explicitly cast to INTEGER to avoid a -245 SQLCODE,
//		// ambiguous parameter.
//		registerFunction( "transform", new SQLFunctionTemplate(
//				geolatteGemetryType,
//				"DB2GSE.ST_Transform(?1, CAST (?2 AS INTEGER))"
//		) );

		registerFunction( "geomFromText", new StandardSQLFunction(
				"DB2GSE.ST_GeomFromText"
		) );

//		// Register spatial aggregate function
//		registerFunction( "extent", new SQLFunctionTemplate(
//				geolatteGemetryType,
//				"db2gse.ST_GetAggrResult(MAX(db2gse.st_BuildMBRAggr(?1)))"
//		) );
	}

	@Override
	public String getDWithinSQL(String columnName) {
		return "db2gse.ST_Intersects(" + columnName + ", db2gse.ST_Buffer(?, ?, 'METER')) = 1";
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return "( db2gse.ST_srsid(" + columnName + ") = ?)";
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		if ( isEmpty ) {
			return "( db2gse.ST_IsEmpty(" + columnName + ") = 1)";
		}
		else {
			return "( db2gse.ST_IsEmpty(" + columnName + ") = 0)";
		}
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int type) {
		switch ( type ) {
			case SpatialAggregate.EXTENT:  // same as extent function above???
				return "db2gse.ST_GetAggrResult(MAX(db2gse.st_BuildMBRAggr(" + columnName + ")))";
			case SpatialAggregate.UNION:
				return "db2gse.ST_GetAggrResult(MAX(db2gse.st_BuildUnionAggr(" + columnName + ")))";
			default:
				throw new IllegalArgumentException(
						"Aggregation of type "
								+ type + " are not supported by this dialect"
				);
		}
	}

	@Override
	public String getSpatialFilterExpression(String arg0) {
		throw new UnsupportedOperationException( "DB2 Dialect doesn't support spatial filtering" );
	}

	//Temporary Fix for HHH-6074
	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		if ( code == 3000 ) {
			return "DB2GSE.ST_GEOMETRY";
		}
		return super.getTypeName( code, length, precision, scale );
	}

	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		String relationName = spatialRelationNames.get( spatialRelation );
		if ( relationName != null ) {
			if ( spatialRelation != SpatialRelation.DISJOINT ) {
				return " db2gse." + relationName + "(" + columnName + ", ?) = 1 SELECTIVITY .0001";
			}
			else { // SELECTIVITY not supported for ST_Disjoint UDF
				return " db2gse." + relationName + "(" + columnName + ", ?) = 1";
			}
		}
		else {
			throw new IllegalArgumentException(
					"Spatial relation " + spatialRelation + " not implemented" );
		}
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return ( getFunctions().get( function.toString() ) != null );
	}

	@Override
	public boolean supportsFiltering() {
		return false;
	}


	private static class DWithinFunction extends StandardSQLFunction {

		public DWithinFunction() {
			super( "db2gse.ST_Dwithin" , StandardBasicTypes.NUMERIC_BOOLEAN);
		}

		public String render(Type firstArgumentType, final List args, final SessionFactoryImplementor factory) {
			StringBuilder sb = new StringBuilder( "db2gse.ST_Intersects( " );
			sb.append( (String)args.get(0) ) //
					.append(", db2gse.ST_Buffer(")
					.append((String)args.get(1) )
					.append(", ")
					.append((String)args.get(2) )
					.append(", 'METER'))");
			return sb.toString();
		}

	}
}
