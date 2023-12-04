/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import org.hibernate.boot.models.categorize.spi.AggregatedKeyMapping;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;

/**
 * @author Steve Ebersole
 */
public class AggregatedKeyMappingImpl implements AggregatedKeyMapping {
	private final AttributeMetadata attribute;

	public AggregatedKeyMappingImpl(AttributeMetadata attribute) {
		this.attribute = attribute;
	}

	@Override
	public AttributeMetadata getAttribute() {
		return attribute;
	}


}
