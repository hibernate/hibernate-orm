/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.lazynocascade;

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
