/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the source mapping of plural-attribute (collection) foreign-key information.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeKeySource
		extends ForeignKeyContributingSource,
				RelationalValueSourceContainer {
	String getReferencedPropertyName();
	boolean isCascadeDeleteEnabled();
}
