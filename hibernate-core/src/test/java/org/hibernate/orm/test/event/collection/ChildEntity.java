/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection;


/**
 *
 * @author Gail Badner
 */
public class ChildEntity extends ChildValue implements Entity {
	private Long id;

	public ChildEntity() {
		super();
	}

	public ChildEntity(String name) {
		super( name );
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
