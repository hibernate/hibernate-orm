/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.mapping.Table;

/**
 * @author Andrea Boriero
 */
public class NameSpaceTablesInformation {
	private final IdentifierHelper identifierHelper;
	private final Map<String, TableInformation> tables = new HashMap<>();

	public NameSpaceTablesInformation(IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addTableInformation(TableInformation tableInformation) {
		tables.put( tableInformation.getName().getTableName().getText(), tableInformation );
	}

	public @Nullable TableInformation getTableInformation(Table table) {
		return tables.get( identifierHelper.toMetaDataObjectName( table.getQualifiedTableName().getTableName() ) );
	}

	public @Nullable TableInformation getTableInformation(String tableName) {
		return tables.get( tableName );
	}
}
