//$Id: Range.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.util;

public final class Range {

	public static int[] range(int begin, int length) {
		int[] result = new int[length];
		for ( int i=0; i<length; i++ ) {
			result[i]=begin + i;
		}
		return result;
	}

	private Range() {}
}







