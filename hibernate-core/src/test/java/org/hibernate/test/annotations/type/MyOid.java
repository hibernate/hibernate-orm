//$Id$
package org.hibernate.test.annotations.type;
import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class MyOid implements Serializable {
	private int high;
	private int middle;
	private int low;
	private int other;

	protected MyOid() {
	}

	public MyOid(int aHigh, int aMiddle, int aLow, int aOther) {
		high = aHigh;
		middle = aMiddle;
		low = aLow;
		other = aOther;
	}

	public int getHigh() {
		return high;
	}

	public void setHigh(int aHigh) {
		high = aHigh;
	}

	public int getMiddle() {
		return middle;
	}

	public void setMiddle(int aMiddle) {
		middle = aMiddle;
	}

	public int getLow() {
		return low;
	}

	public void setLow(int aLow) {
		low = aLow;
	}

	public int getOther() {
		return other;
	}

	public void setOther(int aOther) {
		other = aOther;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final MyOid myOid = (MyOid) o;

		if ( high != myOid.high ) return false;
		if ( low != myOid.low ) return false;
		if ( middle != myOid.middle ) return false;
		if ( other != myOid.other ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = low;
		result = 29 * result + middle;
		result = 29 * result + high;
		result = 29 * result + other;
		return result;
	}
}
