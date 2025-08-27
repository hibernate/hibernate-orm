/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Chris Cranford
 */
@Entity
public class EntityWithManyToOneWithoutJoinTable {
	private Integer id;
	private Integer someInteger;
	private EntityWithOneToManyNotOwned owner;

	EntityWithManyToOneWithoutJoinTable() {
	}

	public EntityWithManyToOneWithoutJoinTable(Integer id, Integer someInteger) {
		this.id = id;
		this.someInteger = someInteger;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}

	@ManyToOne
	public EntityWithOneToManyNotOwned getOwner() {
		return owner;
	}

	public void setOwner(EntityWithOneToManyNotOwned owner) {
		this.owner = owner;
	}
}
