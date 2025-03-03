/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.type;
import java.io.Serializable;

import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class MyOid implements Serializable {
	private int aHigh;
	private int aMiddle;
	private int aLow;
	private int aOther;

	protected MyOid() {
	}

	public MyOid(int aHigh, int aMiddle, int aLow, int aOther) {
		this.aHigh = aHigh;
		this.aMiddle = aMiddle;
		this.aLow = aLow;
		this.aOther = aOther;
	}

	public int getaHigh() {
		return aHigh;
	}

	public void setaHigh(int aHigh) {
		this.aHigh = aHigh;
	}

	public int getaMiddle() {
		return aMiddle;
	}

	public void setaMiddle(int aMiddle) {
		this.aMiddle = aMiddle;
	}

	public int getaLow() {
		return aLow;
	}

	public void setaLow(int aLow) {
		this.aLow = aLow;
	}

	public int getaOther() {
		return aOther;
	}

	public void setaOther(int aOther) {
		this.aOther = aOther;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final MyOid myOid = (MyOid) o;

		if ( aHigh != myOid.aHigh ) return false;
		if ( aLow != myOid.aLow ) return false;
		if ( aMiddle != myOid.aMiddle ) return false;
		if ( aOther != myOid.aOther ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = aLow;
		result = 29 * result + aMiddle;
		result = 29 * result + aHigh;
		result = 29 * result + aOther;
		return result;
	}
}
