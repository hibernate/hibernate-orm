//$Id: ComparableComparator.java 4496 2004-09-04 13:01:55Z oneovthafew $
package org.hibernate.util;

import java.util.Comparator;

/**
 * Delegates to Comparable
 * @author Gavin King
 */
public class ComparableComparator implements Comparator {

	public int compare(Object x, Object y) {
		return ( (Comparable) x ).compareTo(y);
	}
	
	public static final Comparator INSTANCE = new ComparableComparator();

}
