/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
