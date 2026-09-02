/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.spi.SqlAppender;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Appends the database-specific non-identity part of a column definition.
///
/// Append type, collation, default, generated, and nullability syntax in the
/// database-required order. Do not append the column name, identity, unique,
/// check, comment, or mapping-option clauses and do not retain either argument.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getColumnDefinitionSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ColumnDefinitionSupport {
	default void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		requireNonNull( appender );
		requireNonNull( request );
		appender.appendSql( ' ' );
		appender.appendSql( request.sqlType() );
		if ( request.renderedCollation() != null ) {
			appender.appendSql( " collate " );
			appender.appendSql( request.renderedCollation() );
		}
		if ( request.defaultExpression() != null ) {
			appender.appendSql( " default " );
			appender.appendSql( request.defaultExpression() );
		}
		if ( request.generatedExpression() != null ) {
			appender.appendSql( " generated always as (" );
			appender.appendSql( request.generatedExpression() );
			appender.appendSql( ") stored" );
		}
		if ( !request.nullable() ) {
			appender.appendSql( " not null" );
		}
	}
}
