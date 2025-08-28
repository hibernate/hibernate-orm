/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;


/**
 * Created by IntelliJ IDEA.
 * User: Gail
 * Date: Jan 2, 2007
 * Time: 4:52:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeleteOrphanChild {
	private Long id;
	private Parent parent;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
