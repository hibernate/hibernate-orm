/*
 * SPDX-License-Identifier: Apache-2.0
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
