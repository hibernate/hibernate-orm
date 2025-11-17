/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the nature of plural attribute elements in terms of relational implications.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public enum PluralAttributeElementNature {
	/**
	 * The collection elements are basic, simple values.
	 */
	BASIC( false, false ),
	/**
	 * The collection elements are compositions.
	 */
	AGGREGATE( false, true ),
	/**
	 * The collection elements represent entity's in a one-to-many association.
	 */
	ONE_TO_MANY,
	/**
	 * The collection elements represent entity's in a many-to-many association.
	 */
	MANY_TO_MANY,
	/**
	 * The collection elements represent entity's in a multi-valued ANY mapping.
	 */
	MANY_TO_ANY;

	private final boolean isAssociation;
	private final boolean isCascadeable;

	PluralAttributeElementNature() {
		this( true, true );
	}

	PluralAttributeElementNature(boolean association, boolean cascadeable) {
		this.isAssociation = association;
		this.isCascadeable = cascadeable;
	}

	public boolean isAssociation() {
		return isAssociation;
	}

	public boolean isCascadeable() {
		return isCascadeable;
	}
}
