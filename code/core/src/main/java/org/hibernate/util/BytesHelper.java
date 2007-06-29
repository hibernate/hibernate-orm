//$Id: BytesHelper.java 4597 2004-09-26 03:17:21Z oneovthafew $
package org.hibernate.util;

public final class BytesHelper {

	private BytesHelper() {}

	public static int toInt( byte[] bytes ) {
		int result = 0;
		for (int i=0; i<4; i++) {
			result = ( result << 8 ) - Byte.MIN_VALUE + (int) bytes[i];
		}
		return result;
	}
	
}






