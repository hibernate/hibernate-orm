/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.compare;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Delegates to Comparable
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ComparableComparator<T extends Comparable> implements Comparator<T>, Serializable {
	public static final Comparator INSTANCE = new ComparableComparator();

	@SuppressWarnings({ "unchecked" })
	public int compare(Comparable one, Comparable another) {
		return one.compareTo( another );
	}
}
