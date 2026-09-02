/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.MappingSettings.PREFERRED_ARRAY_JDBC_TYPE;
import static org.hibernate.cfg.MappingSettings.PREFERRED_BOOLEAN_JDBC_TYPE;

/// Verifies preferred array and Boolean type-code supply and precedence.
///
/// @author Steve Ebersole
public class PreferredSqlTypeCodeTest {
	@Test
	void configurationTakesPrecedenceOverDialectDefaults() {
		final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting( DIALECT, SQLServerDialect.class )
				.applySetting( PREFERRED_ARRAY_JDBC_TYPE, "TABLE" )
				.applySetting( PREFERRED_BOOLEAN_JDBC_TYPE, "SMALLINT" )
				.build();
		try {
			assertThat( ConfigurationHelper.getPreferredSqlTypeCodeForArray( registry ) )
					.isEqualTo( SqlTypes.TABLE );
			assertThat( ConfigurationHelper.getPreferredSqlTypeCodeForBoolean( registry ) )
					.isEqualTo( SqlTypes.SMALLINT );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void defaultsAndMaintainedOverridesPreserveTheirValues() {
		final Dialect root = new Dialect( DatabaseVersion.make( 1 ) ) {};
		assertThat( root.getPreferredSqlTypeCodeForArray() ).isEqualTo( SqlTypes.VARBINARY );
		assertThat( root.getPreferredSqlTypeCodeForBoolean() ).isEqualTo( SqlTypes.BOOLEAN );
		assertThat( new MySQLDialect().getPreferredSqlTypeCodeForArray() ).isEqualTo( SqlTypes.JSON_ARRAY );
		assertThat( new DB2Dialect().getPreferredSqlTypeCodeForArray() ).isEqualTo( SqlTypes.XML_ARRAY );
		assertThat( new OracleDialect().getPreferredSqlTypeCodeForArray() ).isEqualTo( SqlTypes.ARRAY );
		assertThat( new MySQLDialect().getPreferredSqlTypeCodeForBoolean() ).isEqualTo( SqlTypes.BIT );
		assertThat( new OracleDialect( DatabaseVersion.make( 22 ) ).getPreferredSqlTypeCodeForBoolean() )
				.isEqualTo( SqlTypes.BIT );
		assertThat( new OracleDialect( DatabaseVersion.make( 23 ) ).getPreferredSqlTypeCodeForBoolean() )
				.isEqualTo( SqlTypes.BOOLEAN );
	}

}
