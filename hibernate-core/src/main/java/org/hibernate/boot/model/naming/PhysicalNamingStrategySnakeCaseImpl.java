/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

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
	public PhysicalName toPhysicalCatalogName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalSchemaName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalTableName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalSequenceName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalColumnName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalTypeName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalPrimaryKeyName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalForeignKeyName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalUniqueKeyName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	@Override
	public PhysicalName toPhysicalIndexName(LogicalName logicalName, PhysicalNamingContext context) {
		return apply( logicalName, context );
	}

	private PhysicalName apply(LogicalName name, PhysicalNamingContext context) {
		if ( name == null ) {
			return null;
		}
		else if ( name.isQuoted() ) {
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

	protected PhysicalName unquotedIdentifier(LogicalName name, PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( camelCaseToSnakeCase( name.getText() ).toLowerCase( Locale.ROOT ), false );
	}

	protected PhysicalName quotedIdentifier(LogicalName quotedName, PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( quotedName.getText(), true );
	}

	private boolean isUnderscoreRequired(final char before, final char current, final char after) {
		return ( isLowerCase( before ) || isDigit( before ) )
			&& isUpperCase( current )
			&& ( isLowerCase( after ) || isDigit( after ) );
	}
}
