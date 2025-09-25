/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.dynamic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "BasicEntity")
@Table(name = "BasicEntity")
public class BasicEntity {
	@Id
	private Integer id;
	private String name;
	private int position;

	@ManyToOne
	OtherEntity other;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public OtherEntity getOther() {
		return other;
	}

	public void setOther(OtherEntity other) {
		this.other = other;
	}
}
