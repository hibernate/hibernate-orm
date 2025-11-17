/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.overridden;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Oliver Breidenbach
 */
@MappedSuperclass
@Access(AccessType.PROPERTY)
public abstract class AbstractProduct {
	private Long id;
	private String name;

	protected AbstractProduct() {
	}

	protected AbstractProduct(String name) {
		this.name = name;
	}
	@Id
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}
	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
