//$Id$
package org.hibernate.test.legacy;


/**
 * @author Gavin King
 */
public class Down extends Up {

	private long value;

	public long getValue() {
		return value;
	}

	public void setValue(long l) {
		value = l;
	}

}
