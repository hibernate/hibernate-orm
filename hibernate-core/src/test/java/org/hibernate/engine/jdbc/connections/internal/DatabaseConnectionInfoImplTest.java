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
	void redactPasswordWithoutChangingTheOriginalUrl() {
		final String url = "jdbc:mariadb://host/database?user=graphql&password=secret&useSsl=true";
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
				.contains( "password=***" )
				.doesNotContain( "password=secret" );
		assertThat( info.getJdbcUrl() ).isEqualTo( url );
		assertThat( DatabaseConnectionInfoImpl.redactJdbcUrl( url ) )
				.isEqualTo( "jdbc:mariadb://host/database?user=graphql&password=***&useSsl=true" );
		assertThat( url ).contains( "password=secret" );
	}

	@Test
	void redactSensitiveParametersCaseInsensitively() {
		assertThat( DatabaseConnectionInfoImpl.redactJdbcUrl(
				"jdbc:mariadb://host/database?PASSWORD=secret;pwd=other&token=value&name=kept" ) )
				.isEqualTo( "jdbc:mariadb://host/database?PASSWORD=***;pwd=***&token=***&name=kept" );
	}

	@Test
	void preserveEncodedParameterValuesAndUrlsWithoutQueryParameters() {
		assertThat( DatabaseConnectionInfoImpl.redactJdbcUrl(
				"jdbc:mariadb://host/database?password=a%26b&name=value" ) )
				.isEqualTo( "jdbc:mariadb://host/database?password=***&name=value" );
		assertThat( DatabaseConnectionInfoImpl.redactJdbcUrl( "jdbc:mariadb://host/database" ) )
				.isEqualTo( "jdbc:mariadb://host/database" );
	}

	@Test
	void redactCredentialsInUserInfo() {
		assertThat( DatabaseConnectionInfoImpl.redactJdbcUrl( "jdbc:mariadb://user:secret@host/database" ) )
				.isEqualTo( "jdbc:mariadb://user:***@host/database" );
	}
}
