//$Id: EqualsHelper.java 4582 2004-09-25 11:22:20Z oneovthafew $
package org.hibernate.util;

/**
 * @author Gavin King
 */
public final class EqualsHelper {

	public static boolean equals(Object x, Object y) {
		return x==y || ( x!=null && y!=null && x.equals(y) );
	}
	
	private EqualsHelper() {}

}
