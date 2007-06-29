package org.hibernate.test.legacy;

import java.io.Serializable;

/**
 * @author Stefano Travelli
 */
public class MiddleKey implements Serializable {
	private Inner sup;
	private String one;
	private String two;

	public String getOne() {
		return one;
	}

	public void setOne(String one) {
		this.one = one;
	}

	public String getTwo() {
		return two;
	}

	public void setTwo(String two) {
		this.two = two;
	}

	public Inner getSup() {
		return sup;
	}

	public void setSup(Inner sup) {
		this.sup = sup;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MiddleKey)) return false;

		final MiddleKey cidMasterID = (MiddleKey) o;

		if (one != null ? !one.equals(cidMasterID.one) : cidMasterID.one != null) return false;
		if (sup != null ? !sup.equals(cidMasterID.sup) : cidMasterID.sup != null) return false;
		if (two != null ? !two.equals(cidMasterID.two) : cidMasterID.two != null) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (sup != null ? sup.hashCode() : 0);
		result = 29 * result + (one != null ? one.hashCode() : 0);
		result = 29 * result + (two != null ? two.hashCode() : 0);
		return result;
	}
}
