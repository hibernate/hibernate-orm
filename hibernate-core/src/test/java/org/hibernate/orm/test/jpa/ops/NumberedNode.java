/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;


/**
 * @author Gavin King
 */
public class NumberedNode extends Node {

	private long id;

	public NumberedNode() {
		super();
	}


	public NumberedNode(String name) {
		super( name );
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
