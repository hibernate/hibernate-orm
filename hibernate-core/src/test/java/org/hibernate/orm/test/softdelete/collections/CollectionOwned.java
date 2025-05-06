/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "coll_owned")
public class CollectionOwned {
	@Id
	private Integer id;
	@Basic
	private String name;

	public CollectionOwned() {
	}

	public CollectionOwned(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}
