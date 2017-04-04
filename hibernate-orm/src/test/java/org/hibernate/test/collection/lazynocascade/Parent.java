/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.lazynocascade;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Vasily Kochnev
 */
public class Parent {
	private Long id;

	// LinkedHashSet used for the reason to force the specific order of elements in collection
	private Set<BaseChild> children = new LinkedHashSet<BaseChild>();

	/**
	 * @return Entity identifier.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id Identifier to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return Set of children entities.
	 */
	public Set<BaseChild> getChildren() {
		return children;
	}

	/**
	 * @param children Set of children entities to set.
	 */
	public void setChildren(Set<BaseChild> children) {
		this.children = children;
	}
}
