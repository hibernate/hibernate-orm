/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;


/// Standard BasicKeyMapping impl
///
/// @since 9.0
/// @author Steve Ebersole
public record BasicKeyMappingImpl(AttributeMetadata attribute) implements BasicKeyMapping {
	@Override
	public AttributeMetadata getAttribute() {
		return attribute;
	}
}
