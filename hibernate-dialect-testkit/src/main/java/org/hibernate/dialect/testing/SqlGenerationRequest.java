/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing;

import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Inputs for translating one HQL statement through the test kit's fixed
/// internal mapping model.
///
/// Only named parameters are supported. Parameter values establish binding
/// types and are never sent to a database.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public record SqlGenerationRequest(
		String hql,
		@Nullable Class<?> expectedResultType,
		Map<String, Object> parameterValues,
		@Nullable Pagination pagination,
		@Nullable LockMode lockMode) {
	public SqlGenerationRequest {
		hql = Objects.requireNonNull( hql, "hql" ).strip();
		if ( hql.isEmpty() ) {
			throw new IllegalArgumentException( "hql must not be blank" );
		}
		parameterValues = Map.copyOf( Objects.requireNonNull( parameterValues, "parameterValues" ) );
	}

	/// Create the simplest request for an HQL statement.
	public static SqlGenerationRequest hql(String hql) {
		return new SqlGenerationRequest( hql, null, Map.of(), null, null );
	}
}
