/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.io.Serializable;
import java.util.Set;

public class Fee implements Serializable {
	public Fee fee;
	public Fee anotherFee;
	public String fi;
	public String key;
	public Set fees;
	private Qux qux;
	private FooComponent compon;
	private int count;

	public Fee() {
	}

	public Fee getFee() {
		return fee;
	}

	public void setFee(Fee fee) {
		this.fee = fee;
	}

	public String getFi() {
		return fi;
	}

	public void setFi(String fi) {
		this.fi = fi;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Set getFees() {
		return fees;
	}

	public void setFees(Set fees) {
		this.fees = fees;
	}

	public Fee getAnotherFee() {
		return anotherFee;
	}

	public void setAnotherFee(Fee anotherFee) {
		this.anotherFee = anotherFee;
	}

	public Qux getQux() {
		return qux;
	}

	public void setQux(Qux qux) {
		this.qux = qux;
	}

	public FooComponent getCompon() {
		return compon;
	}

	public void setCompon(FooComponent compon) {
		this.compon = compon;
	}

	/**
	 * Returns the count.
	 * @return int
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Sets the count.
	 * @param count The count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

}
