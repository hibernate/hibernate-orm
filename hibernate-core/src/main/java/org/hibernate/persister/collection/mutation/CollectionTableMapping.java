/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.spi.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableKeyDescriptor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.sql.model.TableMapping;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class CollectionTableMapping extends CollectionTableDescriptor implements TableMapping {
	private final String[] spaces;

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
		this(
				tableName,
				null,
				spaces,
				isJoinTable,
				isInverse,
				insertDetails,
				updateDetails,
				cascadeDeleteEnabled,
				deleteAllDetails,
				deleteRowDetails
		);
	}

	public CollectionTableMapping(
			String tableName,
			NavigableRole navigableRole,
			String[] spaces,
			boolean isJoinTable,
			boolean isInverse,
			MutationDetails insertDetails,
			MutationDetails updateDetails,
			boolean cascadeDeleteEnabled,
			MutationDetails deleteAllDetails,
			MutationDetails deleteRowDetails) {
		super(
				tableName,
				navigableRole,
				isJoinTable,
				isInverse,
				cascadeDeleteEnabled,
				insertDetails,
				updateDetails,
				deleteRowDetails,
				deleteAllDetails
		);
		this.spaces = spaces;
	}

	/**
	 * Creates an auxiliary table mapping (for history or audit tables)
	 * based on an existing collection table mapping.
	 */
	public CollectionTableMapping(CollectionTableMapping baseMapping, String tableName) {
		this( baseMapping, tableName, appendSpace( baseMapping.spaces, tableName ) );
	}

	public CollectionTableMapping(
			CollectionTableMapping baseMapping,
			String tableName,
			String[] spaces) {
		this(
				tableName,
				baseMapping.navigableRole(),
				spaces,
				baseMapping.isJoinTable(),
				baseMapping.isInverse(),
				baseMapping.getInsertDetails(),
				baseMapping.getUpdateDetails(),
				baseMapping.isCascadeDeleteEnabled(),
				baseMapping.getDeleteDetails(),
				baseMapping.getDeleteRowDetails()
		);
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
		return name();
	}

	public String[] getSpaces() {
		return spaces;
	}

	@Override
	public boolean containsTableName(String tableName) {
		if ( getTableName().equals( tableName ) ) {
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

	public void initializeDescriptor(
			boolean isSelfReferential,
			boolean hasUniqueConstraints,
			TableKeyDescriptor keyDescriptor) {
		initializeGraphDetails(
				isSelfReferential,
				hasUniqueConstraints,
				keyDescriptor
		);
	}

	public boolean hasDescriptorDetails() {
		return hasGraphDetails();
	}

	@Override
	public boolean isJoinTable() {
		return super.isJoinTable();
	}

	@Override
	public int relativePosition() {
		return 0;
	}

	@Override
	public boolean isOptional() {
		return false;
	}

	@Override
	public boolean isInverse() {
		return super.isInverse();
	}

	@Override
	public boolean isIdentifierTable() {
		// if there is an id (id-bag), the collection table would hold it
		return true;
	}

	@Override
	public MutationDetails getInsertDetails() {
		return insertDetails();
	}

	@Override
	public MutationDetails getUpdateDetails() {
		return updateDetails();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled();
	}

	@Override
	public MutationDetails getDeleteDetails() {
		return deleteAllDetails();
	}

	public MutationDetails getDeleteRowDetails() {
		return deleteDetails();
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof CollectionTableMapping that) ) {
			return false;
		}
		else {
			return getTableName().equals( that.getTableName() );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( getTableName() );
	}
}
