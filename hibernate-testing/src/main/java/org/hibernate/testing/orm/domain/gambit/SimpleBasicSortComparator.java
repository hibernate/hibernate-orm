/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import java.util.Comparator;

/**
 * @author Nathan Xu
 */
public class SimpleBasicSortComparator implements Comparator<String> {

	@Override
	public int compare(String s1, String s2) {
		return String.CASE_INSENSITIVE_ORDER.compare( s1, s2 );
	}
}
