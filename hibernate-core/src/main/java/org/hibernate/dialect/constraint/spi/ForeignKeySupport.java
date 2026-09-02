/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.constraint.spi;

import org.hibernate.SPI;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines database support for foreign-key DDL and delete semantics.
///
/// Implement this strategy when a database changes the add/drop grammar,
/// supports only selected `on delete` actions, suppresses post-creation foreign
/// keys, or requires self-referential keys to be nullified before deletion.
/// Returned fragments contain no leading or trailing whitespace.
///
/// @see Dialect#getForeignKeySupport()
///
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ForeignKeySupport {
	default boolean supportsAlterTableConstraints() {
		return true;
	}

	default String renderAddConstraint(ForeignKeyConstraintRequest request) {
		requireNonNull( request, "request" );
		final var result = new StringBuilder( "add constraint " )
				.append( request.constraintName() )
				.append( ' ' );
		if ( request.isExplicitDefinition() ) {
			return result.append( request.explicitDefinition() ).toString();
		}
		result.append( "foreign key (" )
				.append( String.join( ", ", request.sourceColumnNames() ) )
				.append( ") references " )
				.append( request.referencedTableName() );
		if ( !request.referencesPrimaryKey() ) {
			result.append( " (" )
					.append( String.join( ", ", request.targetColumnNames() ) )
					.append( ')' );
		}
		return result.toString();
	}

	default String renderDropConstraint(ForeignKeyDropRequest request) {
		requireNonNull( request, "request" );
		return switch ( request.ifExistsPlacement() ) {
			case NONE -> "drop constraint " + request.constraintName();
			case BEFORE_NAME -> "drop constraint if exists " + request.constraintName();
			case AFTER_NAME -> "drop constraint " + request.constraintName() + " if exists";
		};
	}

	default boolean supportsOnDeleteAction(OnDeleteAction action) {
		requireNonNull( action, "action" );
		return true;
	}

	default boolean requiresSelfReferentialForeignKeyNullification() {
		return false;
	}
}
