/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;

public class StringComparator implements Comparator, Serializable {

	public int compare(Object o1, Object o2) {
		return ( (String) o1 ).toLowerCase(Locale.ROOT).compareTo( ( (String) o2 ).toLowerCase(Locale.ROOT) );
	}

}
