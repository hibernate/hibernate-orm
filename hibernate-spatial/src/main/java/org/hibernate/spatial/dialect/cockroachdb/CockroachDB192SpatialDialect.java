/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.cockroachdb;

import java.util.Map;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.CockroachDB192Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.postgis.PGGeometryTypeDescriptor;
import org.hibernate.spatial.dialect.postgis.PostgisSupport;

public class CockroachDB192SpatialDialect extends CockroachDB192Dialect implements SpatialDialect {

	private transient CockroachDBSpatialSupport support = new CockroachDBSpatialSupport();

	public CockroachDB192SpatialDialect(){
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


	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		return support.getSpatialRelateSQL( columnName, spatialRelation );
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
	public String getSpatialFilterExpression(String columnName) {
		return support.getSpatialFilterExpression( columnName );
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		return support.getSpatialAggregateSQL( columnName, aggregation );
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
	public boolean supportsFiltering() {
		return support.supportsFiltering();
	}

	@Override
	public boolean supports(SpatialFunction function) {
		return support.supports( function );
	}

}
