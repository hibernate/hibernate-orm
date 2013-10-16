/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
