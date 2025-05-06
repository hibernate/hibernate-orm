/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultcache;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class AggregateEntity {
	@Id
	private Integer id;
	private String name;
	@ManyToOne(cascade = CascadeType.ALL)
	private TestEntity value1;
	@ManyToOne(cascade = CascadeType.ALL)
	private TestEntity value2;

	protected AggregateEntity() {
	}

	public AggregateEntity(
			Integer id,
			String name,
			TestEntity value1,
			TestEntity value2) {
		this.id = id;
		this.name = name;
		this.value1 = value1;
		this.value2 = value2;
	}

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

	public TestEntity getValue1() {
		return value1;
	}

	public void setValue1(TestEntity value1) {
		this.value1 = value1;
	}

	public TestEntity getValue2() {
		return value2;
	}

	public void setValue2(TestEntity value2) {
		this.value2 = value2;
	}
}
