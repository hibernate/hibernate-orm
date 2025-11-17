/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.util.Locale;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;

/**
 * Converts {@code camelCase} or {@code MixedCase} logical names to {@code snake_case}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
// Originally copied from Spring's SpringPhysicalNamingStrategy as this strategy is popular there.
public class PhysicalNamingStrategySnakeCaseImpl implements PhysicalNamingStrategy {

	@Override
	public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return apply( logicalName );
	}

	@Override
	public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return apply( logicalName );
	}

	@Override
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return apply( logicalName );
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return apply( logicalName );
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return apply( logicalName );
	}

	private Identifier apply(final Identifier name) {
		if ( name == null ) {
			return null;
		}
		else if ( name.isQuoted() ) {
			return quotedIdentifier( name );
		}
		else {
			return unquotedIdentifier( name );
		}
	}

	private String camelCaseToSnakeCase(String name) {
		final StringBuilder builder = new StringBuilder( name.replace( '.', '_' ) );
		for ( int i = 1; i < builder.length() - 1; i++ ) {
			if ( isUnderscoreRequired( builder.charAt( i - 1 ), builder.charAt( i ), builder.charAt( i + 1 ) ) ) {
				builder.insert( i++, '_' );
			}
		}
		return builder.toString();
	}

	protected Identifier unquotedIdentifier(Identifier name) {
		return new Identifier( camelCaseToSnakeCase( name.getText() ).toLowerCase( Locale.ROOT ) );
	}

	protected Identifier quotedIdentifier(Identifier quotedName) {
		return quotedName;
	}

	private boolean isUnderscoreRequired(final char before, final char current, final char after) {
		return ( isLowerCase( before ) || isDigit( before ) )
			&& isUpperCase( current )
			&& ( isLowerCase( after ) || isDigit( after ) );
	}
}
