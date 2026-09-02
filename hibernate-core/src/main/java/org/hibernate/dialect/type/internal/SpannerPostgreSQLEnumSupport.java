/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.internal;

import java.util.Collection;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.type.SqlTypes.isCharacterType;
import static org.hibernate.type.SqlTypes.isIntegral;

/// Built-in Spanner PostgreSQL enum lifecycle and finite-domain checks.
///
/// @author Steve Ebersole
/// @since 8.0
public final class SpannerPostgreSQLEnumSupport implements EnumSupport {
	public static final EnumSupport INSTANCE = new SpannerPostgreSQLEnumSupport();

	private final EnumSupport delegate = EnumSupports.postgresql();

	private SpannerPostgreSQLEnumSupport() {
	}

	@Override
	public @Nullable String getTypeDeclaration(String name, String[] relationalValues) {
		return delegate.getTypeDeclaration( name, relationalValues );
	}

	@Override
	public String[] getCreateTypeCommands(String name, String[] relationalValues) {
		return delegate.getCreateTypeCommands( name, relationalValues );
	}

	@Override
	public String[] getDropTypeCommands(String name) {
		return delegate.getDropTypeCommands( name );
	}

	@Override
	public String getCheckCondition(String columnName, Collection<?> relationalValues, JdbcType jdbcType) {
		final boolean character = isCharacterType( jdbcType.getJdbcTypeCode() );
		if ( !character && !isIntegral( jdbcType.getJdbcTypeCode() ) ) {
			throw new IllegalArgumentException( "Unsupported finite-domain JDBC type: " + jdbcType.getJdbcTypeCode() );
		}
		final var check = new StringBuilder( "(" );
		String separator = "";
		boolean nullIsValid = false;
		for ( Object value : relationalValues ) {
			if ( value == null ) {
				nullIsValid = true;
				continue;
			}
			check.append( separator ).append( columnName ).append( '=' );
			if ( character ) {
				QuotingHelper.appendSingleQuoteEscapedString( check, String.valueOf( value ) );
			}
			else {
				check.append( value );
			}
			separator = " or ";
		}
		check.append( ')' );
		if ( nullIsValid ) {
			check.append( " or " ).append( columnName ).append( " is null" );
		}
		return check.toString();
	}

	@Override
	public String getCheckCondition(String columnName, long min, long max) {
		return delegate.getCheckCondition( columnName, min, max );
	}
}
