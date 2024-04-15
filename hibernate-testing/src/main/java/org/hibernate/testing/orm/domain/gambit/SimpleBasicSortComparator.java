/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
