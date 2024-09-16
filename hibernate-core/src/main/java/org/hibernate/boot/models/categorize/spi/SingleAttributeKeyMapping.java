/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public interface SingleAttributeKeyMapping extends KeyMapping {
	AttributeMetadata getAttribute();

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
