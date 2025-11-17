/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.fetching;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Resource {
	private Long id;
	private String name;
	private User owner;

	public Resource() {
	}

	public Resource(String name, User owner) {
		this.name = name;
		this.owner = owner;
	}

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

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}
}
