/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
