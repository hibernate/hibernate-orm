/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.mapping.Table;

/**
 * @author Andrea Boriero
 */
public class NameSpaceTablesInformation {
	private final IdentifierHelper identifierHelper;
	private Map<String, TableInformation> tables = new HashMap<>();

	public NameSpaceTablesInformation(IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addTableInformation(TableInformation tableInformation) {
		tables.put( tableInformation.getName().getTableName().getText(), tableInformation );
	}

	public TableInformation getTableInformation(Table table) {
		return tables.get( identifierHelper.toMetaDataObjectName( table.getQualifiedTableName().getTableName() ) );
	}

	public TableInformation getTableInformation(String tableName) {
		return tables.get( tableName );
	}
}
