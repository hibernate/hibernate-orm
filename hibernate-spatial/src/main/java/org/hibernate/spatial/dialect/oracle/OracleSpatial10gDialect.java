/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;


import java.io.Serializable;
import java.sql.Types;
import java.util.Map;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.WithCustomJPAFilter;

import org.jboss.logging.Logger;

/**
 * Spatial Dialect for Oracle10g databases.
 *
 * @author Karel Maesen
 */
public class OracleSpatial10gDialect extends Oracle10gDialect implements SpatialDialect, WithCustomJPAFilter, Serializable {

	private static final HSMessageLogger log = Logger.getMessageLogger(
			HSMessageLogger.class,
			OracleSpatial10gDialect.class.getName()
	);


	transient private OracleSDOSupport sdoSupport = new OracleSDOSupport( true );

	/**
	 * Constructs the dialect with
	 */
	public OracleSpatial10gDialect() {
		super();

		// register geometry type
		registerColumnType( Types.STRUCT, "MDSYS.SDO_GEOMETRY" );
		for ( Map.Entry<String, SQLFunction> entry : sdoSupport.functionsToRegister() ) {
			registerFunction( entry.getKey(), entry.getValue() );
		}

	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);
		sdoSupport.contributeTypes( typeContributions, serviceRegistry );
	}


	@Override
	public String getSpatialFilterExpression(String columnName) {
		return sdoSupport.getSpatialFilterExpression( columnName );
	}


	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		return sdoSupport.getSpatialRelateSQL( columnName, spatialRelation );
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		return sdoSupport.getSpatialAggregateSQL( columnName, aggregation );
	}

	@Override
	public String getDWithinSQL(String columnName) {
		return sdoSupport.getDWithinSQL( columnName );
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return sdoSupport.getHavingSridSQL( columnName );
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		return sdoSupport.getIsEmptySQL( columnName, isEmpty );
	}

	@Override
	public boolean supportsFiltering() {
		return sdoSupport.supportsFiltering();
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return ( getFunctions().get( function.toString() ) != null );
	}

	@Override
	public String filterExpression(String geometryParam, String filterParam) {
		return sdoSupport.filterExpression( geometryParam, filterParam );
	}
}
