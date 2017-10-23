/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.lazynocascade;

/**
 * @author Vasily Kochnev
 */
public class BaseChild {
	private Long id;

	private BaseChild dependency;

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
	 * @return Child dependency
	 */
	public BaseChild getDependency() {
		return dependency;
	}

	/**
	 * @param dependency Dependency to set.
	 */
	public void setDependency(BaseChild dependency) {
		this.dependency = dependency;
	}
}
