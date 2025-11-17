/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.inverseToSuperclass;

import java.util.List;

import org.hibernate.envers.Audited;

@Audited
public class DetailSuperclass {

	private long id;

	private List<Root> roots;

	public DetailSuperclass() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<Root> getRoots() {
		return roots;
	}

	public void setRoots(List<Root> roots) {
		this.roots = roots;
	}

}
