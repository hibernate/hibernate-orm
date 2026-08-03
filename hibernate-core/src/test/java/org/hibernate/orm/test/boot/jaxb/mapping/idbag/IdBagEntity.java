/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.idbag;

import java.util.ArrayList;
import java.util.List;

public class IdBagEntity {
	private long id;
	private List<IdBagEntity> children = new ArrayList<>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<IdBagEntity> getChildren() {
		return children;
	}

	public void setChildren(List<IdBagEntity> children) {
		this.children = children;
	}
}
