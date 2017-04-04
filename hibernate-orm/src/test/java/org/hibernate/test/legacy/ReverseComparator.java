/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: ReverseComparator.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.util.Comparator;

public final class ReverseComparator implements Comparator, Serializable {
	public int compare(Object x, Object y) {
		return - ( (Comparable) x ).compareTo(y);
	}
	
	public boolean equals(Object obj) {
		return obj instanceof ReverseComparator;
	}
	
	public int hashCode() {
		return 0;
	}
	
}






