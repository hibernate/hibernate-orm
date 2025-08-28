/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes the nature of the collection itself as declared by the metadata.
 *
 * @author Steve Ebersole
 */
public enum PluralAttributeNature {
	BAG( Collection.class, false ),
	ID_BAG( Collection.class, false ),
	SET( Set.class, false ),
	LIST( List.class, true ),
	ARRAY( Object[].class, true ),
	MAP( Map.class, true );

	private final boolean indexed;
	private final Class<?> reportedJavaType;

	PluralAttributeNature(Class<?> reportedJavaType, boolean indexed) {
		this.reportedJavaType = reportedJavaType;
		this.indexed = indexed;
	}

	public Class<?> reportedJavaType() {
		return reportedJavaType;
	}

	public boolean isIndexed() {
		return indexed;
	}
}
