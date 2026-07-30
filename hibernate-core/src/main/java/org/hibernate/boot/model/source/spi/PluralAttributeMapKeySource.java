/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * Describes source information about the key of a persistent map.
 *
 * @author Steve Ebersole
 *
 * @see PluralAttributeMapKeyManyToManySource
 * @see PluralAttributeMapKeyManyToAnySource
 */
@Remove
public interface PluralAttributeMapKeySource extends PluralAttributeIndexSource {
	enum Nature {
		BASIC,
		EMBEDDED,
		MANY_TO_MANY,
		ANY
	}

	Nature getMapKeyNature();
}
