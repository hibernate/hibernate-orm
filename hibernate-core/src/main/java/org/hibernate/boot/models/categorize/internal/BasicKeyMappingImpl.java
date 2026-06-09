/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;

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
