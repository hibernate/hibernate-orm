/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.util.Set;

public class One {
	Long key;
	String value;
	Set manies;
	private int x;

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public void setKey(Long key) {
		this.key = key;
	}

	public Long getKey() {
		return this.key;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public Set getManies() {
		return manies;
	}

	public void setManies(Set manies) {
		this.manies = manies;
	}

}
