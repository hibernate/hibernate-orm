/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.manytomany;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrea Boriero
 */
public class User implements Serializable {

	private Long id;
	private Set groups = new HashSet();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set getGroups() {
		return groups;
	}

	public void setGroups(Set groups) {
		this.groups = groups;
	}
}
