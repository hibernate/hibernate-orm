/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.models.spi.ClassDetails;

/// Key mapping represented by one persistent attribute.
///
/// The attribute may be a simple value, as with {@link BasicKeyMapping}, or an
/// embeddable value, as with {@link AggregatedKeyMapping}.
///
/// @since 9.0
/// @author Steve Ebersole
public interface SingleAttributeKeyMapping extends KeyMapping {
	/// The persistent attribute that represents this key.
	AttributeMetadata getAttribute();

	/// The name of the persistent attribute that represents this key.
	default String getAttributeName() {
		return getAttribute().getName();
	}

	default ClassDetails getKeyType() {
		return getAttribute().getMember().getType().determineRawClass();
	}

	@Override
	default void forEachAttribute(AttributeConsumer consumer) {
		consumer.accept( 0, getAttribute() );
	}

	@Override
	default boolean contains(AttributeMetadata attributeMetadata) {
		return attributeMetadata == getAttribute();
	}
}
