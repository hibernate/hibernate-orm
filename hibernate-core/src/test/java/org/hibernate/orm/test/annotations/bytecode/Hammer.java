/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.bytecode;


/**
 * @author Emmanuel Bernard
 */
public class Hammer implements Tool {
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer usage() {
		return 0;
	}
}
