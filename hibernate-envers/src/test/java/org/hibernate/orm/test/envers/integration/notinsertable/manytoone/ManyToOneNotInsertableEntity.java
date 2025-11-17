/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.notinsertable.manytoone;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

@Entity
@Table(name = "ManyToOneNotIns")
@Audited
public class ManyToOneNotInsertableEntity {
	@Id
	private Integer id;

	@Basic
	@Column(name = "numVal")
	private Integer number;

	@ManyToOne
	@JoinColumn(name = "numVal", insertable = false, updatable = false)
	private NotInsertableEntityType type;

	public ManyToOneNotInsertableEntity() {
	}

	public ManyToOneNotInsertableEntity(Integer id, Integer number, NotInsertableEntityType type) {
		this.id = id;
		this.number = number;
		this.type = type;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public NotInsertableEntityType getType() {
		return type;
	}

	public void setType(NotInsertableEntityType type) {
		this.type = type;
	}
}
