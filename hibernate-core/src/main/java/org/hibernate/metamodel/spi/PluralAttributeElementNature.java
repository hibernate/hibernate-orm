/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi;

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
