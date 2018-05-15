/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;

public class StringComparator implements Comparator, Serializable {

	public int compare(Object o1, Object o2) {
		return ( (String) o1 ).toLowerCase(Locale.ROOT).compareTo( ( (String) o2 ).toLowerCase(Locale.ROOT) );
	}

}
