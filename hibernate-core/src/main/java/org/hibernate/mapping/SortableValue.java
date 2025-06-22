/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

public interface SortableValue {

	boolean isSorted();

	int[] sortProperties();
}
