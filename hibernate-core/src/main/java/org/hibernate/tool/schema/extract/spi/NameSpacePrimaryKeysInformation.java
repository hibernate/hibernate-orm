/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.mapping.Table;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 7.2
 */
public class NameSpacePrimaryKeysInformation {
	private final IdentifierHelper identifierHelper;
	private final Map<String, PrimaryKeyInformation> primaryKeys = new HashMap<>();

	public NameSpacePrimaryKeysInformation(IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addPrimaryKeyInformation(TableInformation tableInformation, PrimaryKeyInformation primaryKeyInformation) {
		primaryKeys.put( tableInformation.getName().getTableName().getText(), primaryKeyInformation );
	}

	public @Nullable PrimaryKeyInformation getPrimaryKeyInformation(Table table) {
		return primaryKeys.get( identifierHelper.toMetaDataObjectName( table.getQualifiedTableName().getTableName() ) );
	}

	public @Nullable PrimaryKeyInformation getPrimaryKeyInformation(String tableName) {
		return primaryKeys.get( tableName );
	}

	public void validate() {
		for ( Map.Entry<String, PrimaryKeyInformation> entry : primaryKeys.entrySet() ) {
			final var tableName = entry.getKey();
			final var primaryKeyInformation = entry.getValue();
			int i = 1;
			for ( ColumnInformation column : primaryKeyInformation.getColumns() ) {
				if ( column == null ) {
					throw new SchemaExtractionException(
							"Primary Key information was missing for key [" +
							primaryKeyInformation.getPrimaryKeyIdentifier() + "] on table [" + tableName +
							"] at KEY_SEQ = " + i
					);
				}
				i++;
			}
		}
	}
}
