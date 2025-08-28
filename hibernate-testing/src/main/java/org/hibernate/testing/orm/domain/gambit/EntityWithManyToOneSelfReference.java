/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "entity_m2o_selfref")
@SuppressWarnings("unused")
public class EntityWithManyToOneSelfReference {
	private Integer id;

	// alphabetical
	private String name;
	private EntityWithManyToOneSelfReference other;
	private Integer someInteger;

	EntityWithManyToOneSelfReference() {
	}

	public EntityWithManyToOneSelfReference(Integer id, String name, Integer someInteger) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
	}

	public EntityWithManyToOneSelfReference(
			Integer id,
			String name,
			Integer someInteger,
			EntityWithManyToOneSelfReference other) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
		this.other = other;
	}

	public EntityWithManyToOneSelfReference(
			Integer id,
			String name,
			EntityWithManyToOneSelfReference other,
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

	@ManyToOne
	@JoinColumn
	public EntityWithManyToOneSelfReference getOther() {
		return other;
	}

	public void setOther(EntityWithManyToOneSelfReference other) {
		this.other = other;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}
}
