/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.query;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

@Entity(name = "Bar")
@NamedQueries({
		@NamedQuery(name = Bar.FIND_ALL, query = "select b from Bar b")
})
public class Bar {
	public static final String FIND_ALL = "Bar.findAll";

	@EmbeddedId
	private BarPK id;
	private String name;

	public BarPK getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
