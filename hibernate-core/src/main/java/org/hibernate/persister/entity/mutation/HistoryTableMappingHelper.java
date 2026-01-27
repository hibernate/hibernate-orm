/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Utility for building history table mappings that mirror the identifier table mapping.
 */
final class HistoryTableMappingHelper {
	private HistoryTableMappingHelper() {
	}

	static EntityTableMapping createHistoryTableMapping(
			EntityTableMapping identifierTableMapping,
			EntityPersister persister,
			String historyTableName) {
		return new EntityTableMapping(
				historyTableName,
				identifierTableMapping.getRelativePosition(),
				identifierTableMapping.getKeyMapping(),
				identifierTableMapping.isOptional(),
				identifierTableMapping.isInverse(),
				identifierTableMapping.isIdentifierTable(),
				identifierTableMapping.getAttributeIndexes(),
				identifierTableMapping.getInsertExpectation(),
				identifierTableMapping.getInsertCustomSql(),
				identifierTableMapping.isInsertCallable(),
				identifierTableMapping.getUpdateExpectation(),
				identifierTableMapping.getUpdateCustomSql(),
				identifierTableMapping.isUpdateCallable(),
				identifierTableMapping.isCascadeDeleteEnabled(),
				identifierTableMapping.getDeleteExpectation(),
				identifierTableMapping.getDeleteCustomSql(),
				identifierTableMapping.isDeleteCallable(),
				persister.isDynamicUpdate(),
				persister.isDynamicInsert()
		);
	}
}
