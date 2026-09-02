/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identifier.spi;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Supplies the environment-specific inputs used to build an identifier helper.
///
/// Mutate [#builder()] only while processing this request. Do not retain this
/// request, its builder, or its JDBC metadata after returning the helper.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record IdentifierHelperBuildRequest(
		IdentifierHelperBuilder builder,
		JdbcMetadata jdbcMetadata,
		KeywordSupport keywordSupport,
		NameQualifierSupport nameQualifierSupport) {

	public IdentifierHelperBuildRequest {
		requireNonNull( builder, "builder" );
		requireNonNull( jdbcMetadata, "jdbcMetadata" );
		requireNonNull( keywordSupport, "keywordSupport" );
		requireNonNull( nameQualifierSupport, "nameQualifierSupport" );
	}
}
