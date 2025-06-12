/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.Set;

public class Parent {
	private Integer id;
	private Set<DeleteOrphanChild> deleteOrphanChildren;
	private Set<Child> children;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<DeleteOrphanChild> getDeleteOrphanChildren() {
		return deleteOrphanChildren;
	}

	public void setDeleteOrphanChildren(Set<DeleteOrphanChild> deleteOrphanChildren) {
		this.deleteOrphanChildren = deleteOrphanChildren;
	}

	public Set<Child> getChildren() {
		return children;
	}

	public void setChildren(Set<Child> children) {
		this.children = children;
	}
}
