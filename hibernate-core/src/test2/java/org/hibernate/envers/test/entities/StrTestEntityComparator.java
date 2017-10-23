/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities;

import java.util.Comparator;

public class StrTestEntityComparator implements Comparator<StrTestEntity> {
	public static final StrTestEntityComparator INSTANCE = new StrTestEntityComparator();

	@Override
	public int compare(StrTestEntity o1, StrTestEntity o2) {
		return o1.getStr().compareTo( o2.getStr() );
	}
}
