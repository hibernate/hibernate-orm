package org.hibernate.test.cascade;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Gail
 * Date: Jan 2, 2007
 * Time: 4:50:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class Parent {
	private Long id;
	private Set deleteOrphanChildren;
	private Set children;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set getDeleteOrphanChildren() {
		return deleteOrphanChildren;
	}

	public void setDeleteOrphanChildren(Set deleteOrphanChildren) {
		this.deleteOrphanChildren = deleteOrphanChildren;
	}

	public Set getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}
}
