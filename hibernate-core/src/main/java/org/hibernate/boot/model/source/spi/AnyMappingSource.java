/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * Base description for all discriminated associations ("any mappings"), including
 * {@code <any/>}, {@code <many-to-any/>}, etc.
 *
 * @author Steve Ebersole
 */
@Remove
public interface AnyMappingSource {
	AnyDiscriminatorSource getDiscriminatorSource();
	AnyKeySource getKeySource();

	default boolean isLazy() {
		return true;
	}
}
