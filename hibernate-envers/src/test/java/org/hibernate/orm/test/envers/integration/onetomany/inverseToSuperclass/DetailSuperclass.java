/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.inverseToSuperclass;

import org.hibernate.envers.Audited;

@Audited
public class DetailSuperclass {

	private long id;

	private Root parent;

	public DetailSuperclass() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Root getParent() {
		return parent;
	}

	public void setParent(Root parent) {
		this.parent = parent;
	}

}
