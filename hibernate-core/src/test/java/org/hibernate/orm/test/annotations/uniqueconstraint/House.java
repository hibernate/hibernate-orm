/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * @author Manuel Bernhardt
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "uniqueWithInherited", columnNames = {"room_id", "cost"} )})
public class House extends Building {
	@Column(nullable = false)
	public Long id;
	@NotNull
	public Integer cost;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getCost() {
		return cost;
	}

	public void setCost(Integer cost) {
		this.cost = cost;
	}
}
