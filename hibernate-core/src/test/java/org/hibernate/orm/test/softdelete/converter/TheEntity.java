/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.converter;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "the_entity")
@SoftDelete(converter = YesNoConverter.class)
public class TheEntity {
	@Id
	private Integer id;
	@Basic
	private String name;

	protected TheEntity() {
		// for Hibernate use
	}

	public TheEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
