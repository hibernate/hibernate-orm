/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade2;


/**
 * todo: describe ChildInfo
 *
 * @author Steve Ebersole
 */
public class ChildInfoAssigned {
	private Long id;
	private ChildAssigned owner;
	private String info;

	public ChildInfoAssigned() {
	}

	public ChildInfoAssigned(Long id, String info) {
		this.id = id;
		this.info = info;
	}

	public Long getId() {
		return id;
	}

	public ChildAssigned getOwner() {
		return owner;
	}

	public void setOwner(ChildAssigned owner) {
		this.owner = owner;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}
