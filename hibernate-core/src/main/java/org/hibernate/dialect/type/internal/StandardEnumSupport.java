/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.type.SqlTypes.isCharacterType;
import static org.hibernate.type.SqlTypes.isIntegral;

/// Built-in immutable enum-support profiles.
///
/// @author Steve Ebersole
/// @since 8.0
public final class StandardEnumSupport implements EnumSupport {
	private enum Kind { STANDARD, INLINE, H2, POSTGRESQL, ORACLE }

	private static final StandardEnumSupport STANDARD = new StandardEnumSupport( Kind.STANDARD, null );
	private static final StandardEnumSupport INLINE = new StandardEnumSupport( Kind.INLINE, null );
	private static final StandardEnumSupport H2 = new StandardEnumSupport( Kind.H2, null );
	private static final StandardEnumSupport POSTGRESQL = new StandardEnumSupport( Kind.POSTGRESQL, null );
	private static final Map<DatabaseVersion, EnumSupport> ORACLE = new ConcurrentHashMap<>();

	private final Kind kind;
	private final DatabaseVersion version;

	private StandardEnumSupport(Kind kind, @Nullable DatabaseVersion version) {
		this.kind = kind;
		this.version = version;
	}

	public static EnumSupport standard() {
		return STANDARD;
	}

	public static EnumSupport inline() {
		return INLINE;
	}

	public static EnumSupport h2() {
		return H2;
	}

	public static EnumSupport postgresql() {
		return POSTGRESQL;
	}

	public static EnumSupport oracle(DatabaseVersion version) {
		if ( version == null ) {
			throw new IllegalArgumentException( "Database version must not be null" );
		}
		return ORACLE.computeIfAbsent( version, key -> new StandardEnumSupport( Kind.ORACLE, key ) );
	}

	@Override
	public @Nullable String getTypeDeclaration(String name, String[] relationalValues) {
		return switch ( kind ) {
			case STANDARD -> null;
			case INLINE, H2 -> inlineDeclaration( relationalValues );
			case POSTGRESQL -> name;
			case ORACLE -> version.isSameOrAfter( 23 ) ? name : null;
		};
	}

	@Override
	public String[] getCreateTypeCommands(String name, String[] relationalValues) {
		return switch ( kind ) {
			case STANDARD, INLINE -> EMPTY_STRING_ARRAY;
			case H2 -> h2CreateCommands( name, relationalValues );
			case POSTGRESQL -> postgresqlCreateCommands( name, relationalValues );
			case ORACLE -> oracleCreateCommands( name, relationalValues );
		};
	}

	@Override
	public String[] getCreateOrdinalTypeCommands(String name, String[] relationalValues) {
		return kind == Kind.ORACLE
				? oracleOrdinalCreateCommands( name, relationalValues )
				: getCreateTypeCommands( name, relationalValues );
	}

	@Override
	public String[] getDropTypeCommands(String name) {
		return switch ( kind ) {
			case STANDARD, INLINE -> EMPTY_STRING_ARRAY;
			case H2 -> new String[] { "drop domain if exists " + name };
			case POSTGRESQL -> new String[] { "drop type if exists " + name + " cascade" };
			case ORACLE -> new String[] { "drop domain if exists " + name + " force" };
		};
	}

	@Override
	public String getCheckCondition(String columnName, Collection<?> relationalValues, JdbcType jdbcType) {
		final boolean character = isCharacterType( jdbcType.getJdbcTypeCode() );
		if ( !character && !isIntegral( jdbcType.getJdbcTypeCode() ) ) {
			throw new IllegalArgumentException( "Unsupported finite-domain JDBC type: " + jdbcType.getJdbcTypeCode() );
		}
		final var check = new StringBuilder( columnName ).append( " in (" );
		String separator = "";
		boolean nullIsValid = false;
		for ( Object value : relationalValues ) {
			if ( value == null ) {
				nullIsValid = true;
				continue;
			}
			check.append( separator );
			if ( character ) {
				QuotingHelper.appendSingleQuoteEscapedString( check, String.valueOf( value ) );
			}
			else {
				check.append( value );
			}
			separator = ",";
		}
		check.append( ')' );
		if ( nullIsValid ) {
			check.append( " or " ).append( columnName ).append( " is null" );
		}
		return check.toString();
	}

	@Override
	public String getCheckCondition(String columnName, long min, long max) {
		return columnName + " between " + min + " and " + max;
	}

	private static String inlineDeclaration(String[] values) {
		return "enum (" + quotedValues( values ) + ')';
	}

	private static String[] h2CreateCommands(String name, String[] values) {
		final int maxLength = Arrays.stream( values ).map( String::length ).max( Integer::compareTo ).orElseThrow();
		return new String[] {
				"create domain " + name + " as varchar(" + maxLength + ") check (value in ("
						+ quotedValues( values ) + "))"
		};
	}

	private static String[] postgresqlCreateCommands(String name, String[] values) {
		return new String[] {
				"create type " + name + " as enum (" + quotedValues( values ) + ')',
				"create cast (varchar as " + name + ") with inout as implicit",
				"create cast (" + name + " as varchar) with inout as implicit"
		};
	}

	private String[] oracleCreateCommands(String name, String[] values) {
		final var domain = new StringBuilder( "create domain " ).append( name ).append( " as enum (" );
		String separator = "";
		for ( String value : values ) {
			domain.append( separator ).append( value ).append( "='" ).append( value ).append( '\'' );
			separator = ", ";
		}
		return new String[] { domain.append( ')' ).toString() };
	}

	private static String[] oracleOrdinalCreateCommands(String name, String[] values) {
		return new String[] { "create domain " + name + " as enum (" + String.join( ", ", values ) + ')' };
	}

	private static String quotedValues(String[] values) {
		final var result = new StringBuilder();
		String separator = "";
		for ( String value : values ) {
			result.append( separator );
			QuotingHelper.appendSingleQuoteEscapedString( result, value );
			separator = ",";
		}
		return result.toString();
	}
}
