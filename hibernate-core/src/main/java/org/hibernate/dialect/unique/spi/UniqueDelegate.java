/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.spi;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Applies logical unique declarations to database DDL.
///
/// A delegate owns all three possible emission points: a single-column
/// definition, the `create table` constraint list, and separate add/drop
/// commands. It also selects whether metadata and DDL represent a logical
/// declaration as a constraint or a unique index. Keep those decisions
/// coherent by consulting [#representation] instead of a separate Dialect
/// capability.
///
/// Implement this contract for provider-specific behavior, decorate a stock
/// strategy with [DelegatingUniqueDelegate], or obtain a standard strategy
/// from [UniqueDelegates]. Return empty fragments or commands when the selected
/// emission point does not apply. Do not retain the mutable mapping arguments.
///
/// Hibernate requires null values in nullable unique declarations to be
/// distinct. A delegate for a database with different native semantics must
/// use a suitable index, suppress an unsupported nullable declaration, or
/// otherwise preserve that behavior.
///
/// @see Dialect#getUniqueDelegate()
///
/// @author Brett Meyer
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface UniqueDelegate {
	/// Select whether a logical unique declaration is represented as a
	/// constraint or an index.
	default UniqueKeyRepresentation representation(UniqueKeyRepresentationRequest request) {
		requireNonNull( request, "request" );
		return request.containsFormula() || request.explicitType() || request.explicitUsing()
				? UniqueKeyRepresentation.INDEX
				: UniqueKeyRepresentation.CONSTRAINT;
	}

	/// Whether this delegate can render `nulls not distinct` for a requested
	/// unique constraint.
	default boolean supportsNullsNotDistinct() {
		return false;
	}
	/// Render the column-definition uniqueness fragment for a column explicitly
	/// marked [Column#isUnique() unique]. Return an empty string when uniqueness
	/// belongs at another emission point.
	String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context);

	/// Render all unique declarations embedded in `create table`. Include the
	/// required leading comma, or return an empty string when no declaration is
	/// emitted at this point.
	String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context);

	/// Render the complete command which adds the unique key. Return an empty
	/// string when it was already emitted during table creation or is unsupported.
	String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context);

	/// Render the complete command which drops the unique key previously emitted
	/// by [#getAlterTableToAddUniqueKeyCommand]. Return an empty string when no
	/// corresponding database object was created.
	String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context);

}
