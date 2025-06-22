/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sorted;

import java.util.Comparator;

/**
 * Serves as a common {@link Comparator<T>} util class to test {@link org.hibernate.annotations.SortComparator SortComparator}
 * annotation. Basically a {@link String#CASE_INSENSITIVE_ORDER} wrapper because {@code @SortComparator}
 * annotation only supports {@code class} value property, not {@code class} instance.
 *
 * @author Nathan Xu
 * @see org.hibernate.annotations.SortComparator
 */
public class StringCaseInsensitiveComparator implements Comparator<String> {

	@Override
	public int compare(String s1, String s2) {
		return String.CASE_INSENSITIVE_ORDER.compare( s1, s2 );
	}

}
