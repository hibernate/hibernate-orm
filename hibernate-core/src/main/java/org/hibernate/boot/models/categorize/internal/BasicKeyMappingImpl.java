/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;

/**
 * @author Steve Ebersole
 */
public class BasicKeyMappingImpl implements BasicKeyMapping {
	private final AttributeMetadata attribute;

	public BasicKeyMappingImpl(AttributeMetadata attribute) {
		this.attribute = attribute;
	}

	@Override
	public AttributeMetadata getAttribute() {
		return attribute;
	}
}
