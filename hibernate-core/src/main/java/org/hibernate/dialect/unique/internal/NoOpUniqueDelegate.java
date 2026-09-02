/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.internal;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/// Hibernate-owned profile which suppresses logical unique-key DDL.
///
/// @author Steve Ebersole
/// @since 8.0
@Internal
public final class NoOpUniqueDelegate implements UniqueDelegate {
	public static final NoOpUniqueDelegate INSTANCE = new NoOpUniqueDelegate();

	private NoOpUniqueDelegate() {
	}

	@Override
	public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
		return "";
	}

	@Override
	public String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context) {
		return "";
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(
			UniqueKey uniqueKey,
			Metadata metadata,
			SqlStringGenerationContext context) {
		return "";
	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(
			UniqueKey uniqueKey,
			Metadata metadata,
			SqlStringGenerationContext context) {
		return "";
	}
}
