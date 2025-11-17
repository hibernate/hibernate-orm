/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the source for the elements of persistent collections (plural
 * attributes) where the elements are many-to-many association
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementSourceManyToMany
		extends PluralAttributeElementSourceAssociation, RelationalValueSourceContainer,
				ForeignKeyContributingSource, Orderable {
	String getReferencedEntityName();

	String getReferencedEntityAttributeName();

	boolean isIgnoreNotFound();

	String getExplicitForeignKeyName();

	boolean isUnique();

	FilterSource[] getFilterSources();

	String getWhere();

	FetchCharacteristics getFetchCharacteristics();
}
