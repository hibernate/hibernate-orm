/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.subselect;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Sharath Reddy
 */
@Entity
public class Item {

	private long id;
	private String name;

	@Id
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}


}
