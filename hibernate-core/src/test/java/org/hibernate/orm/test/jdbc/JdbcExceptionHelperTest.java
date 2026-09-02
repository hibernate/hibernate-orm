/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc;

import java.sql.SQLException;

import org.hibernate.jdbc.spi.JdbcExceptionHelper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// Tests extraction of error information from JDBC exception chains.
///
/// @author Steve Ebersole
public class JdbcExceptionHelperTest {
	@Test
	void extractsInformationFromTheJdbcExceptionChain() {
		final SQLException first = new SQLException( "first", null, 0 );
		final SQLException second = new SQLException( "second", "23ABC", 0 );
		final SQLException third = new SQLException( "third", "42ABC", 57 );
		first.setNextException( second );
		second.setNextException( third );

		assertThat( JdbcExceptionHelper.extractErrorCode( first ) ).isEqualTo( 57 );
		assertThat( JdbcExceptionHelper.extractSqlState( first ) ).isEqualTo( "23ABC" );
		assertThat( JdbcExceptionHelper.extractSqlStateClassCode( first ) ).isEqualTo( "23" );
		assertThat( JdbcExceptionHelper.determineSqlStateClassCode( "X" ) ).isEqualTo( "X" );
		assertThat( JdbcExceptionHelper.determineSqlStateClassCode( null ) ).isNull();

		final SQLException causeOnly = new SQLException( "outer" );
		causeOnly.initCause( new SQLException( "cause", "ZZ999", 99 ) );
		assertThat( JdbcExceptionHelper.extractErrorCode( causeOnly ) ).isZero();
		assertThat( JdbcExceptionHelper.extractSqlState( causeOnly ) ).isNull();
		assertThatNullPointerException().isThrownBy( () -> JdbcExceptionHelper.extractErrorCode( null ) );
	}
}
