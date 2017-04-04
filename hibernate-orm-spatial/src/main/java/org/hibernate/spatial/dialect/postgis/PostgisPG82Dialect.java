/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import java.util.Map;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;

/**
 * Extends the {@code PostgreSQL82Dialect} to add support for the Postgis spatial types, functions and operators .
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 */
public class PostgisPG82Dialect extends PostgreSQL82Dialect implements SpatialDialect {


	transient private PostgisSupport support = new PostgisSupport();

	/**
	 * Creates an instance
	 */
	public PostgisPG82Dialect() {
		super();
		registerColumnType(
				PGGeometryTypeDescriptor.INSTANCE.getSqlType(),
				"GEOMETRY"
		);
		for ( Map.Entry<String, SQLFunction> entry : support.functionsToRegister() ) {
			registerFunction( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(
				typeContributions,
				serviceRegistry
		);
		support.contributeTypes( typeContributions, serviceRegistry );
	}


	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		return support.getSpatialRelateSQL( columnName, spatialRelation );
	}

	@Override
	public String getDWithinSQL(String columnName) {
		return support.getDWithinSQL( columnName );
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return support.getHavingSridSQL( columnName );
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		return support.getIsEmptySQL( columnName, isEmpty );
	}

	@Override
	public String getSpatialFilterExpression(String columnName) {
		return support.getSpatialFilterExpression( columnName );
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		return support.getSpatialAggregateSQL( columnName, aggregation );
	}

	@Override
	public boolean supportsFiltering() {
		return support.supportsFiltering();
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return support.supports( function );
	}
}
