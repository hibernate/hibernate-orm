/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Function;

public interface SortableValue {

	boolean isSorted();

	default int[] sortProperties() {
		return sortProperties( null );
	}

	int[] sortProperties(Function<String, PersistentClass> entityBindingResolver);
}
