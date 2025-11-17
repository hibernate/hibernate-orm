/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class CorrectChildId implements Serializable {
	@Column(name = "parent_id")
	private String id;

	@Column(name = "child_number")
	private Integer number;

	CorrectChildId() {

	}

	public CorrectChildId(Integer number, Parent parent) {
		this.number = number;
		this.id = parent.getId();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}
}
