/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade2;


/**
 * todo: describe Other
 *
 * @author Steve Ebersole
 */
public class OtherAssigned {
	private Long id;
	private ParentAssigned owner;

	public OtherAssigned() {
	}

	public OtherAssigned(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public ParentAssigned getOwner() {
		return owner;
	}

	public void setOwner(ParentAssigned owner) {
		this.owner = owner;
	}
}
