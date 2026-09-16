/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import org.junit.jupiter.api.Test;

import org.hibernate.dialect.H2Dialect;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectionInfoImplTest {
	@Test
	void omitMariaDbJdbcParametersFromLoggingWithoutChangingTheOriginalUrl() {
		final String url = "jdbc:mariadb://host/database?user=dbUsername&password=dbPassword&useSsl=true";
		final DatabaseConnectionInfoImpl info = new DatabaseConnectionInfoImpl(
				null,
				url,
				"MariaDB Connector/J",
				H2Dialect.class,
				new H2Dialect().getVersion(),
				true,
				true,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		assertThat( info.toInfoString() )
				.contains( "Database JDBC URL [jdbc:mariadb://host/database]" )
				.doesNotContain( "dbUsername" )
				.doesNotContain( "dbPassword" );
		assertThat( info.getJdbcUrl() ).isEqualTo( url );
		assertThat( url ).contains( "password=dbPassword" );
	}

	@Test
	void preserveJdbcParametersForOtherDrivers() {
		final String url = "jdbc:h2:mem:test?password=secret";
		final DatabaseConnectionInfoImpl info = new DatabaseConnectionInfoImpl(
				null,
				url,
				"H2 JDBC Driver",
				H2Dialect.class,
				new H2Dialect().getVersion(),
				true,
				true,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		assertThat( info.toInfoString() ).contains( "Database JDBC URL [" + url + "]" );
	}

	@Test
	void omitMariaDbJdbcParametersWhenUsingMysqlScheme() {
		final String url = "jdbc:mysql://host/database?permitMysqlScheme=true&user=dbUsername&password=dbPassword";
		final DatabaseConnectionInfoImpl info = new DatabaseConnectionInfoImpl(
				null,
				url,
				"MySQL Connector/J",
				H2Dialect.class,
				new H2Dialect().getVersion(),
				true,
				true,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		assertThat( info.toInfoString() )
				.contains( "Database JDBC URL [jdbc:mysql://host/database]" )
				.doesNotContain( "dbUsername" )
				.doesNotContain( "dbPassword" );
	}

	@Test
	void preserveJdbcParametersForMysqlSchemeWithoutMariaDbFlag() {
		final String url = "jdbc:mysql://host/database?user=dbUsername&password=dbPassword";
		final DatabaseConnectionInfoImpl info = new DatabaseConnectionInfoImpl(
				null,
				url,
				"MySQL Connector/J",
				H2Dialect.class,
				new H2Dialect().getVersion(),
				true,
				true,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		assertThat( info.toInfoString() ).contains( "Database JDBC URL [" + url + "]" );
	}
}
