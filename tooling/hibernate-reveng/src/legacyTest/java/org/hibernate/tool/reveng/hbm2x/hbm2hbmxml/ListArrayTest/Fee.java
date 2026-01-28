/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.ListArrayTest;

import java.io.ObjectStreamClass;
import java.io.Serial;
import java.io.Serializable;

public class Fee implements Serializable {

	@Serial
	private static final long serialVersionUID =
			ObjectStreamClass.lookup(Fee.class).getSerialVersionUID();

	public Fee anotherFee;
	public String fi;
	public String key;
	private FooComponent compon;
	private int count;

	public Fee() {
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

	public Fee getAnotherFee() {
		return anotherFee;
	}

	public void setAnotherFee(Fee anotherFee) {
		this.anotherFee = anotherFee;
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
