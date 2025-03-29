/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade2;


/**
 * Parent, but with an assigned identifier.
 *
 * @author Steve Ebersole
 */
public class ParentAssigned {
	private Long id;
	private String name;
	private ParentInfoAssigned info;

	public ParentAssigned() {
	}

	public ParentAssigned(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ParentInfoAssigned getInfo() {
		return info;
	}

	public void setInfo(ParentInfoAssigned info) {
		this.info = info;
	}
}
