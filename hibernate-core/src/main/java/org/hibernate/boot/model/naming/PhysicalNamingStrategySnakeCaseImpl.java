/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import java.util.Locale;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;

/// Converts unquoted camel-case logical names to snake case while retaining
/// explicitly quoted names. Global and automatic quoting are applied afterward.
///
/// @author Steve Ebersole
/// @author Phillip Webb
/// @author Madhura Bhave
// Originally copied from Spring's SpringPhysicalNamingStrategy as this strategy is popular there.
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class PhysicalNamingStrategySnakeCaseImpl implements PhysicalNamingStrategy {
	@SPI(SPI.Role.USE)
	public PhysicalNamingStrategySnakeCaseImpl() {
	}


	@Override
	@Nullable
	public PhysicalName toPhysicalCatalogName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return logicalName == null ? null : apply( logicalName, context );
	}

	@Override
	@Nullable
	public PhysicalName toPhysicalSchemaName(@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return logicalName == null ? null : apply( logicalName, context );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTableName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalSequenceName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalColumnName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTypeName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalPrimaryKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalForeignKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalIndexName(@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Nonnull
	private PhysicalName apply(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		if ( name.isQuoted() ) {
			return quotedIdentifier( name, context );
		}
		else {
			return unquotedIdentifier( name, context );
		}
	}

	private String camelCaseToSnakeCase(String name) {
		final var builder = new StringBuilder( name.replace( '.', '_' ) );
		for ( int i = 1; i < builder.length() - 1; i++ ) {
			if ( isUnderscoreRequired( builder.charAt( i - 1 ), builder.charAt( i ), builder.charAt( i + 1 ) ) ) {
				builder.insert( i++, '_' );
			}
		}
		return builder.toString();
	}

	@Nonnull
	protected PhysicalName unquotedIdentifier(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( camelCaseToSnakeCase( name.getText() ).toLowerCase( Locale.ROOT ), false );
	}

	@Nonnull
	protected PhysicalName quotedIdentifier(@Nonnull LogicalName quotedName, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( quotedName.getText(), true );
	}

	private boolean isUnderscoreRequired(final char before, final char current, final char after) {
		return ( isLowerCase( before ) || isDigit( before ) )
			&& isUpperCase( current )
			&& ( isLowerCase( after ) || isDigit( after ) );
	}
}
