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

public class NameSpaceIndexesInformation {
	private final IdentifierHelper identifierHelper;
	private final Map<String, List<IndexInformation>> indexes = new HashMap<>();

	public NameSpaceIndexesInformation(IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addIndexInformation(TableInformation tableInformation, IndexInformation indexInformation) {
		indexes.computeIfAbsent( tableInformation.getName().getTableName().getText(), k -> new ArrayList<>() )
				.add( indexInformation );
	}

	public @Nullable List<IndexInformation> getIndexesInformation(Table table) {
		return indexes.get( identifierHelper.toMetaDataObjectName( table.getQualifiedTableName().getTableName() ) );
	}

	public @Nullable List<IndexInformation> getIndexesInformation(String tableName) {
		return indexes.get( tableName );
	}
}
