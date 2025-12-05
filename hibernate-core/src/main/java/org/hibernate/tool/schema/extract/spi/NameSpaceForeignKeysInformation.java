/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.mapping.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 7.2
 */
public class NameSpaceForeignKeysInformation {
	private final IdentifierHelper identifierHelper;
	private final Map<String, List<ForeignKeyInformation>> foreignKeys = new HashMap<>();

	public NameSpaceForeignKeysInformation(IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addForeignKeyInformation(TableInformation tableInformation, ForeignKeyInformation foreignKeyInformation) {
		foreignKeys.computeIfAbsent( tableInformation.getName().getTableName().getText(), k -> new ArrayList<>() )
				.add( foreignKeyInformation );
	}

	public @Nullable List<ForeignKeyInformation> getForeignKeysInformation(Table table) {
		return foreignKeys.get( identifierHelper.toMetaDataObjectName( table.getQualifiedTableName().getTableName() ) );
	}

	public @Nullable List<ForeignKeyInformation> getForeignKeysInformation(String tableName) {
		return foreignKeys.get( tableName );
	}
}
