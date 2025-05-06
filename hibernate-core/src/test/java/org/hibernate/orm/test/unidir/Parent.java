/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unidir;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class Parent {
	private String name;
	private long ssn;
	private List children = new ArrayList();
	Parent() {}
	public Parent(String name, long ssn) {
		this.name = name;
		this.ssn = ssn;
	}
	public List getChildren() {
		return children;
	}
	public void setChildren(List children) {
		this.children = children;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getSsn() {
		return ssn;
	}
	public void setSsn(long ssn) {
		this.ssn = ssn;
	}
}
