/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

/**
 * Utility for building history table mappings for collection tables.
 */
final class HistoryCollectionTableMappingHelper {
	private HistoryCollectionTableMappingHelper() {
	}

	static CollectionTableMapping createHistoryTableMapping(
			CollectionTableMapping baseMapping,
			String historyTableName) {
		return new CollectionTableMapping(
				historyTableName,
				appendHistorySpace( baseMapping.getSpaces(), historyTableName ),
				baseMapping.isJoinTable(),
				baseMapping.isInverse(),
				baseMapping.getInsertDetails(),
				baseMapping.getUpdateDetails(),
				baseMapping.isCascadeDeleteEnabled(),
				baseMapping.getDeleteDetails(),
				baseMapping.getDeleteRowDetails()
		);
	}

	private static String[] appendHistorySpace(String[] baseSpaces, String historyTableName) {
		for ( String space : baseSpaces ) {
			if ( historyTableName.equals( space ) ) {
				return baseSpaces;
			}
		}
		final String[] spaces = new String[baseSpaces.length + 1];
		System.arraycopy( baseSpaces, 0, spaces, 0, baseSpaces.length );
		spaces[baseSpaces.length] = historyTableName;
		return spaces;
	}
}
