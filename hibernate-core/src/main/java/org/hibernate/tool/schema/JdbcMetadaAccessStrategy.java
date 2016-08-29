/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Andrea Boriero
 */
public enum JdbcMetadaAccessStrategy {
	/**
	 * The {@link org.hibernate.tool.schema.spi.SchemaMigrator} and {@link org.hibernate.tool.schema.spi.SchemaValidator}
	 * execute one {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call for each
	 * {@link javax.persistence.Entity} in order to determine if a corresponding database table exists.
	 */
	INDIVIDUALLY( "individually" ),

	/**
	 * The {@link org.hibernate.tool.schema.spi.SchemaMigrator} and {@link org.hibernate.tool.schema.spi.SchemaValidator}
	 * execute a single {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call
	 * to retrieve all the database table in order to determine all the {@link javax.persistence.Entity} have a mapped database tables.
	 */
	GROUPED( "grouped" );

	private final String strategy;

	JdbcMetadaAccessStrategy(String strategy) {
		this.strategy = strategy;
	}

	@Override
	public String toString() {
		return strategy;
	}

	public static JdbcMetadaAccessStrategy interpretHbm2ddlSetting(Object value) {
		if(value == null){
			return GROUPED;
		}
		String name = value.toString();
		if ( StringHelper.isEmpty( name ) || GROUPED.strategy.equals( name ) ) {
			return GROUPED;
		}
		else if ( INDIVIDUALLY.strategy.equals( name ) ) {
			return INDIVIDUALLY;
		}
		else {
			throw new IllegalArgumentException( "Unrecognized `" + AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY + "` value : " + name );
		}

	}
}
