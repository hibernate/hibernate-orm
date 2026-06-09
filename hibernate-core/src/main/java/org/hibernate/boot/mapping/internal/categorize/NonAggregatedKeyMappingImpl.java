/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;

import org.hibernate.models.spi.ClassDetails;

/// Standard NonAggregatedKeyMapping impl
///
/// @since 9.0
/// @author Steve Ebersole
public record NonAggregatedKeyMappingImpl(
		List<AttributeMetadata> idAttributes,
		ClassDetails idClassType) implements NonAggregatedKeyMapping {
	@Override
	public List<AttributeMetadata> getIdAttributes() {
		return idAttributes;
	}

	@Override
	public ClassDetails getIdClassType() {
		return idClassType;
	}

	@Override
	public ClassDetails getKeyType() {
		return idClassType;
	}

	@Override
	public void forEachAttribute(AttributeConsumer consumer) {
		for ( int i = 0; i < idAttributes.size(); i++ ) {
			consumer.accept( i, idAttributes.get( i ) );
		}
	}

	@Override
	public boolean contains(AttributeMetadata attributeMetadata) {
		for ( int i = 0; i < idAttributes.size(); i++ ) {
			if ( idAttributes.get( i ) == attributeMetadata ) {
				return true;
			}
		}
		return false;
	}
}
