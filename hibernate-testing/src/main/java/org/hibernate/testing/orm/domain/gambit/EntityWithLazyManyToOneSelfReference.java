/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "entity_lm2o_selfref")
public class EntityWithLazyManyToOneSelfReference {
	private Integer id;

	// alphabetical
	private String name;
	private EntityWithLazyManyToOneSelfReference other;
	private Integer someInteger;

	EntityWithLazyManyToOneSelfReference() {
	}

	public EntityWithLazyManyToOneSelfReference(Integer id, String name, Integer someInteger) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
	}

	public EntityWithLazyManyToOneSelfReference(
			Integer id,
			String name,
			Integer someInteger,
			EntityWithLazyManyToOneSelfReference other) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
		this.other = other;
	}

	public EntityWithLazyManyToOneSelfReference(
			Integer id,
			String name,
			EntityWithLazyManyToOneSelfReference other,
			Integer someInteger) {
		this.id = id;
		this.name = name;
		this.other = other;
		this.someInteger = someInteger;
	}

	@Id
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn
	public EntityWithLazyManyToOneSelfReference getOther() {
		return other;
	}

	public void setOther(EntityWithLazyManyToOneSelfReference other) {
		this.other = other;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}
}
