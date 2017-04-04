/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.util.Comparator;

/**
 * Comparator for things that cannot be compared (in a way we know about).
 *
 * @author Steve Ebersole
 */
public class IncomparableComparator implements Comparator {
	public static final IncomparableComparator INSTANCE = new IncomparableComparator();

	@Override
	@SuppressWarnings("ComparatorMethodParameterNotUsed")
	public int compare(Object o1, Object o2) {
		return 0;
	}
}
