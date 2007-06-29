package org.hibernate.test.legacy;

import java.io.Serializable;

/**
 * @author Stefano Travelli
 */
public class InnerKey implements Serializable {
	private String akey;
	private String bkey;

	public String getAkey() {
		return akey;
	}

	public void setAkey(String akey) {
		this.akey = akey;
	}

	public String getBkey() {
		return bkey;
	}

	public void setBkey(String bkey) {
		this.bkey = bkey;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof InnerKey)) return false;

		final InnerKey cidSuperID = (InnerKey) o;

		if (akey != null ? !akey.equals(cidSuperID.akey) : cidSuperID.akey != null) return false;
		if (bkey != null ? !bkey.equals(cidSuperID.bkey) : cidSuperID.bkey != null) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (akey != null ? akey.hashCode() : 0);
		result = 29 * result + (bkey != null ? bkey.hashCode() : 0);
		return result;
	}
}
