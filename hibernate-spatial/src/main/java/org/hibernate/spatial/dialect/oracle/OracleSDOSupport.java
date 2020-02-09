/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import java.io.Serializable;
import java.util.Locale;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.HibernateSpatialConfigurationSettings;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;
import org.hibernate.spatial.dialect.WithCustomJPAFilter;

import org.jboss.logging.Logger;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;
import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;

/**
 * SDO Geometry support for Oracle dialects
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 */
class OracleSDOSupport implements SpatialDialect, Serializable, WithCustomJPAFilter {

	private static final HSMessageLogger log = Logger.getMessageLogger(
			HSMessageLogger.class,
			OracleSpatial10gDialect.class.getName()
	);

	private final boolean isOgcStrict;
	private final SpatialFunctionsRegistry sdoFunctions;

	OracleSDOSupport(boolean isOgcStrict) {
		this.isOgcStrict = isOgcStrict;
		this.sdoFunctions = new OracleSpatialFunctions( isOgcStrict, this );
	}

	SpatialFunctionsRegistry functionsToRegister() {
		return this.sdoFunctions;
	}

	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final SDOGeometryTypeDescriptor sdoGeometryTypeDescriptor = mkSdoGeometryTypeDescriptor( serviceRegistry );
		typeContributions.contributeType( new GeolatteGeometryType( sdoGeometryTypeDescriptor ) );
		typeContributions.contributeType( new JTSGeometryType( sdoGeometryTypeDescriptor ) );

		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.INSTANCE );
	}

	private SDOGeometryTypeDescriptor mkSdoGeometryTypeDescriptor(ServiceRegistry serviceRegistry) {
		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );

		final ConnectionFinder connectionFinder = strategySelector.resolveStrategy(
				ConnectionFinder.class,
				cfgService.getSetting(
						HibernateSpatialConfigurationSettings.CONNECTION_FINDER,
						String.class,
						"org.geolatte.geom.codec.db.oracle.DefaultConnectionFinder"
				)
		);

		log.connectionFinder( connectionFinder.getClass().getCanonicalName() );

		return new SDOGeometryTypeDescriptor(
				new OracleJDBCTypeFactory(
						connectionFinder
				)
		);
	}

	/**
	 * Returns the SQL fragment for the SQL WHERE-clause when parsing
	 * <code>org.hibernatespatial.criterion.SpatialRelateExpression</code>s
	 * into prepared statements.
	 * <p/>
	 *
	 * @param columnName The name of the geometry-typed column to which the relation is
	 * applied
	 * @param spatialRelation The type of spatial relation (as defined in
	 * <code>SpatialRelation</code>).
	 *
	 * @return SQL fragment  {@code SpatialRelateExpression}
	 */
	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		String sql = getOGCSpatialRelateSQL( columnName, "?", spatialRelation ) + " = 1";
		sql += " and " + columnName + " is not null";
		return sql;
	}

	public String getOGCSpatialRelateSQL(String arg1, String arg2, int spatialRelation) {
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

	/**
	 * Returns the SQL fragment for the SQL WHERE-clause when parsing
	 * <code>org.hibernatespatial.criterion.SpatialRelateExpression</code>s
	 * into prepared statements.
	 * <p/>
	 *
	 * @param columnName The name of the geometry-typed column to which the relation is
	 * applied
	 * @param spatialRelation The type of spatial relation (as defined in
	 * <code>SpatialRelation</code>).
	 *
	 * @return SQL fragment  {@code SpatialRelateExpression}
	 */
	public String getSDOSpatialRelateSQL(String columnName, int spatialRelation) {
		String sql = getNativeSpatialRelateSQL( columnName, "?", spatialRelation ) + " = 1";
		sql += " and " + columnName + " is not null";
		return sql;
	}

	String getNativeSpatialRelateSQL(String arg1, String arg2, int spatialRelation) {
		String mask;
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
		final StringBuilder buffer = new StringBuilder( "CASE SDO_RELATE(" ).append( arg1 )
				.append( "," )
				.append( arg2 )
				.append( ",'mask=" )
				.append( mask )
				.append( "') " );
		if ( !negate ) {
			buffer.append( " WHEN 'TRUE' THEN 1 ELSE 0 END" );
		}
		else {
			buffer.append( " WHEN 'TRUE' THEN 0 ELSE 1 END" );
		}
		return buffer.toString();
	}

	/**
	 * Returns the SQL fragment for the SQL WHERE-expression when parsing
	 * <code>org.hibernate.spatial.criterion.SpatialFilterExpression</code>s
	 * into prepared statements.
	 *
	 * @param columnName The name of the geometry-typed column to which the filter is
	 * be applied
	 *
	 * @return Rhe SQL fragment for the {@code SpatialFilterExpression}
	 */
	@Override
	public String getSpatialFilterExpression(String columnName) {
		final StringBuffer buffer = new StringBuffer( "SDO_FILTER(" );
		buffer.append( columnName );
		buffer.append( ",?) = 'TRUE' " );
		return buffer.toString();
	}

	/**
	 * Returns the SQL fragment for the specfied Spatial aggregate expression.
	 *
	 * @param columnName The name of the Geometry property
	 * @param aggregation The type of <code>SpatialAggregate</code>
	 *
	 * @return The SQL fragment for the projection
	 */
	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
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
		aggregateFunction.append( columnName );
		// TODO tolerance must by configurable
		if ( sa.isAggregateType() ) {
			aggregateFunction.append( ", " ).append( .001 ).append( ")" );
		}
		aggregateFunction.append( ")" );

		return aggregateFunction.toString();
	}

	/**
	 * Returns The SQL fragment when parsing a <code>DWithinExpression</code>.
	 *
	 * @param columnName The geometry column to test against
	 *
	 * @return The SQL fragment when parsing a <code>DWithinExpression</code>.
	 */
	@Override
	public String getDWithinSQL(String columnName) {
		return "SDO_WITHIN_DISTANCE (" + columnName + ",?, ?) = 'TRUE' ";
	}

	/**
	 * Returns the SQL fragment when parsing a <code>HavingSridExpression</code>.
	 *
	 * @param columnName The geometry column to test against
	 *
	 * @return The SQL fragment for a <code>HavingSridExpression</code>.
	 */
	@Override
	public String getHavingSridSQL(String columnName) {
		return String.format( " (MDSYS.ST_GEOMETRY(%s).ST_SRID() = ?)", columnName , Locale.US);
	}

	/**
	 * Returns the SQL fragment when parsing a <code>IsEmptyExpression</code> or
	 * <code>IsNotEmpty</code> expression.
	 *
	 * @param columnName The geometry column
	 * @param isEmpty Whether the geometry is tested for empty or non-empty
	 *
	 * @return The SQL fragment for the isempty function
	 */
	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		return String.format( "( MDSYS.ST_GEOMETRY(%s).ST_ISEMPTY() = %d )", columnName, isEmpty ? 1 : 0 , Locale.US);
	}

	/**
	 * Returns true if this <code>SpatialDialect</code> supports a specific filtering function.
	 * <p> This is intended to signal DB-support for fast window queries, or MBR-overlap queries.</p>
	 *
	 * @return True if filtering is supported
	 */
	@Override
	public boolean supportsFiltering() {
		return true;
	}

	/**
	 * Does this dialect supports the specified <code>SpatialFunction</code>.
	 *
	 * @param function <code>SpatialFunction</code>
	 *
	 * @return True if this <code>SpatialDialect</code> supports the spatial function specified by the function parameter.
	 */
	@Override
	public boolean supports(SpatialFunction function) {
		return false;
	}


	@Override
	public String filterExpression(String geometryParam, String filterParam) {
		return SpatialFunction.filter.name() + "(" + geometryParam + ", " + filterParam + ") = 'TRUE' ";
	}
}
