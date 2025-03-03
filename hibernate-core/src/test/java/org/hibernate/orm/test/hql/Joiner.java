/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * Implementation of Joiner.
 *
 * @author Steve Ebersole
 */
public class Joiner {
	private Long id;
	private String name;
	private String joinedName;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getJoinedName() {
		return joinedName;
	}

	public void setJoinedName(String joinedName) {
		this.joinedName = joinedName;
	}
}
