/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.sql.model.TableMapping;

/**
 * @author Steve Ebersole
 */
public class CollectionTableMapping implements TableMapping {
	private final String tableName;
	private final String[] spaces;
	private final boolean isJoinTable;
	private final boolean isInverse;
	private final MutationDetails insertDetails;
	private final MutationDetails updateDetails;
	private final boolean cascadeDeleteEnabled;
	private final MutationDetails deleteAllDetails;
	private final MutationDetails deleteRowDetails;

//	ForeignKeyDescriptor collectionKey;
//	private final ForeignKeyDescriptor elementFk;
//	private final ForeignKeyDescriptor indexFk;

	public CollectionTableMapping(
			String tableName,
			String[] spaces,
			boolean isJoinTable,
			boolean isInverse,
			MutationDetails insertDetails,
			MutationDetails updateDetails,
			boolean cascadeDeleteEnabled,
			MutationDetails deleteAllDetails,
			MutationDetails deleteRowDetails) {
		this.tableName = tableName;
		this.spaces = spaces;
		this.isJoinTable = isJoinTable;
		this.isInverse = isInverse;
		this.insertDetails = insertDetails;
		this.updateDetails = updateDetails;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
		this.deleteAllDetails = deleteAllDetails;
		this.deleteRowDetails = deleteRowDetails;
	}

	/**
	 * Creates an auxiliary table mapping (for history or audit tables)
	 * based on an existing collection table mapping.
	 */
	public CollectionTableMapping(CollectionTableMapping baseMapping, String tableName) {
		this.tableName = tableName;
		this.spaces = appendSpace( baseMapping.spaces, tableName );
		this.isJoinTable = baseMapping.isJoinTable;
		this.isInverse = baseMapping.isInverse;
		this.insertDetails = baseMapping.insertDetails;
		this.updateDetails = baseMapping.updateDetails;
		this.cascadeDeleteEnabled = baseMapping.cascadeDeleteEnabled;
		this.deleteAllDetails = baseMapping.deleteAllDetails;
		this.deleteRowDetails = baseMapping.deleteRowDetails;
	}

	private static String[] appendSpace(String[] baseSpaces, String newSpace) {
		for ( String space : baseSpaces ) {
			if ( newSpace.equals( space ) ) {
				return baseSpaces;
			}
		}
		final var spaces = new String[baseSpaces.length + 1];
		System.arraycopy( baseSpaces, 0, spaces, 0, baseSpaces.length );
		spaces[baseSpaces.length] = newSpace;
		return spaces;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	public String[] getSpaces() {
		return spaces;
	}

	@Override
	public boolean containsTableName(String tableName) {
		if ( this.tableName.equals( tableName ) ) {
			return true;
		}
		for ( String space : spaces ) {
			if ( space.equals( tableName ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public KeyDetails getKeyDetails() {
		// todo (tuple-cleanup) : implement this
		return null;
	}

	public boolean isJoinTable() {
		return isJoinTable;
	}

	@Override
	public int getRelativePosition() {
		return 0;
	}

	@Override
	public boolean isOptional() {
		return false;
	}

	@Override
	public boolean isInverse() {
		return isInverse;
	}

	@Override
	public boolean isIdentifierTable() {
		// if there is an id (id-bag), the collection table would hold it
		return true;
	}

	@Override
	public MutationDetails getInsertDetails() {
		return insertDetails;
	}

	@Override
	public MutationDetails getUpdateDetails() {
		return updateDetails;
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	@Override
	public MutationDetails getDeleteDetails() {
		return deleteAllDetails;
	}

	public MutationDetails getDeleteRowDetails() {
		return deleteRowDetails;
	}
}
