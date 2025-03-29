/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;
import java.util.Set;

public class File {
	private long id;
	private String name;
	private File parent;
	private boolean deleted;
	private Set children;

	public Set getChildren() {
		return children;
	}
	public void setChildren(Set children) {
		this.children = children;
	}

	public File(String name, File parent) {
		this.name = name;
		this.parent = parent;
	}

	File() {}

	public boolean isDeleted() {
		return deleted;
	}
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public File getParent() {
		return parent;
	}
	public void setParent(File parent) {
		this.parent = parent;
	}

}
