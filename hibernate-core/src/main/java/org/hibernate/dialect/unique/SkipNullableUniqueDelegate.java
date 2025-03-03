/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UniqueKey;

/**
 * A {@link UniqueDelegate} that only creates unique constraints on not-null columns, and ignores requests for
 * uniqueness for nullable columns.
 * <p>
 * Needed because unique constraints on nullable columns in Sybase always consider null values to be non-unique.
 * There is simply no way to create a unique constraint with the semantics we want on a nullable column in Sybase.
 * <p>
 * You might argue that this behavior is bad because if the programmer explicitly specifies an {@code @UniqueKey},
 * then we should damn well respect their wishes. But the simple answer is that the user should have also specified
 * {@code @Column(nullable=false)} if that is what they wanted. A unique key on a nullable column just really doesn't
 * make sense in Sybase, except, perhaps, in some incredibly corner cases.
 *
 * @author Gavin King
 */
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
