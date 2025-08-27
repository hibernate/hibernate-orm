/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;

/**
 * @author Andrea Boriero
 */
@Entity
public class EntityWithManyToOneJoinTable {
	private Integer id;

	// alphabetical
	private String name;
	private SimpleEntity other;
	private Integer someInteger;
	private BasicEntity lazyOther;

	public EntityWithManyToOneJoinTable() {
	}

	public EntityWithManyToOneJoinTable(Integer id, String name, Integer someInteger) {
		this.id = id;
		this.name = name;
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
	@JoinTable(name = "simple_entity_assoc",
			joinColumns =  @JoinColumn( name = "entity_fk"),
			inverseJoinColumns = @JoinColumn(name="simple_fk")
	)
	public SimpleEntity getOther() {
		return other;
	}

	public void setOther(SimpleEntity other) {
		this.other = other;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(name = "basic_entity_assoc",
			joinColumns = @JoinColumn(name="entity_fk"),
			inverseJoinColumns = @JoinColumn(name="basic_fk")
	)
	public BasicEntity getLazyOther() {
		return lazyOther;
	}

	public void setLazyOther(BasicEntity lazyOther) {
		this.lazyOther = lazyOther;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}
}
