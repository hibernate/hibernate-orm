/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.meta;

import org.hibernate.AssertionFailure;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.sql.model.TableMapping.MutationDetails;

import java.io.Serializable;

/// [TableDescriptor] for collection tables.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public class CollectionTableDescriptor implements TableDescriptor, Serializable {
	private final String name;
	private final NavigableRole navigableRole;
	private final boolean joinTable;
	private final boolean inverse;
	private final boolean cascadeDeleteEnabled;
	private final MutationDetails insertDetails;
	private final MutationDetails updateDetails;
	private final MutationDetails deleteDetails;
	private final MutationDetails deleteAllDetails;

	private boolean selfReferential;
	private boolean uniqueConstraints;
	private TableKeyDescriptor keyDescriptor;

	public CollectionTableDescriptor(
			String name,
			NavigableRole navigableRole,
			boolean isJoinTable,
			boolean isInverse,
			boolean isSelfReferential,
			boolean hasUniqueConstraints,
			boolean cascadeDeleteEnabled,
			MutationDetails insertDetails,
			MutationDetails updateDetails,
			MutationDetails deleteDetails,
			MutationDetails deleteAllDetails,
			TableKeyDescriptor keyDescriptor) {
		this(
				name,
				navigableRole,
				isJoinTable,
				isInverse,
				cascadeDeleteEnabled,
				insertDetails,
				updateDetails,
				deleteDetails,
				deleteAllDetails
		);
		initializeGraphDetails(
				isSelfReferential,
				hasUniqueConstraints,
				keyDescriptor
		);
	}

	protected CollectionTableDescriptor(
			String name,
			NavigableRole navigableRole,
			boolean isJoinTable,
			boolean isInverse,
			boolean cascadeDeleteEnabled,
			MutationDetails insertDetails,
			MutationDetails updateDetails,
			MutationDetails deleteDetails,
			MutationDetails deleteAllDetails) {
		this.name = name;
		this.navigableRole = navigableRole;
		this.joinTable = isJoinTable;
		this.inverse = isInverse;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
		this.insertDetails = insertDetails;
		this.updateDetails = updateDetails;
		this.deleteDetails = deleteDetails;
		this.deleteAllDetails = deleteAllDetails;
	}

	protected void initializeGraphDetails(
			boolean isSelfReferential,
			boolean hasUniqueConstraints,
			TableKeyDescriptor keyDescriptor) {
		if ( hasGraphDetails() ) {
			throw new AssertionFailure( "Collection table descriptor was already initialized" );
		}
		selfReferential = isSelfReferential;
		uniqueConstraints = hasUniqueConstraints;
		this.keyDescriptor = keyDescriptor;
	}

	protected boolean hasGraphDetails() {
		return keyDescriptor != null;
	}

	private void checkGraphDetailsInitialized() {
		if ( !hasGraphDetails() ) {
			throw new AssertionFailure( "Collection table descriptor was not initialized" );
		}
	}

	@Override
	public String name() {
		return name;
	}

	public NavigableRole navigableRole() {
		return navigableRole;
	}

	public boolean isJoinTable() {
		return joinTable;
	}

	public boolean isInverse() {
		return inverse;
	}

	@Override
	public boolean isSelfReferential() {
		checkGraphDetailsInitialized();
		return selfReferential;
	}

	@Override
	public boolean hasUniqueConstraints() {
		checkGraphDetailsInitialized();
		return uniqueConstraints;
	}

	@Override
	public boolean cascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	@Override
	public MutationDetails insertDetails() {
		return insertDetails;
	}

	@Override
	public MutationDetails updateDetails() {
		return updateDetails;
	}

	@Override
	public MutationDetails deleteDetails() {
		return deleteDetails;
	}

	public MutationDetails deleteAllDetails() {
		return deleteAllDetails;
	}

	@Override
	public TableKeyDescriptor keyDescriptor() {
		checkGraphDetailsInitialized();
		return keyDescriptor;
	}

	@Override
	public boolean isOptional() {
		return false;
	}

	@Override
	public int getRelativePosition() {
		return 0;
	}

	@Override
	public String toString() {
		return "CollectionTableDescriptor[" +
				"name=" + name +
				", navigableRole=" + navigableRole +
				", isJoinTable=" + joinTable +
				", isInverse=" + inverse +
				']';
	}
}
