/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes source information about the key of a persistent map.
 *
 * @author Steve Ebersole
 *
 * @see PluralAttributeMapKeyManyToManySource
 * @see PluralAttributeMapKeyManyToAnySource
 */
public interface PluralAttributeMapKeySource extends PluralAttributeIndexSource {
	enum Nature {
		BASIC,
		EMBEDDED,
		MANY_TO_MANY,
		ANY
	}

	/**
	 * @deprecated No longer used
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	Nature getMapKeyNature();
}
