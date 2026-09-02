/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.internal;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UniqueKey;

/// Suppresses unique declarations involving nullable columns when the database
/// cannot provide Hibernate's required null-distinct semantics.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
@Internal
public class SkipNullableUniqueDelegate extends CreateTableUniqueDelegate {
	public SkipNullableUniqueDelegate(Dialect dialect) {
		super( dialect );
	}

	@Override
	public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
		return column.isNullable() ? "" : super.getColumnDefinitionUniquenessFragment(column, context);
	}

	@Override
	protected void appendUniqueConstraint(StringBuilder fragment, UniqueKey uniqueKey) {
		if ( !uniqueKey.hasNullableColumn() ) {
			super.appendUniqueConstraint( fragment, uniqueKey );
		}
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
		return uniqueKey.hasNullableColumn() ? "" : super.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata, context );
	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
		return uniqueKey.hasNullableColumn() ? "" : super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context );
	}
}
