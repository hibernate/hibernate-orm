/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultcache;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@Entity
public class TestEntity {
	@Id
	private Integer id;
	private String name;
	@Temporal( TemporalType.TIMESTAMP )
	private Instant instant;

	public TestEntity() {
	}

	public TestEntity(Integer id, String name) {
		this( id, name, Instant.now() );
	}

	public TestEntity(Integer id, String name, Instant instant) {
		this.id = id;
		this.name = name;
		this.instant = instant;
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

	public Instant getInstant() {
		return instant;
	}

	public void setInstant(Instant instant) {
		this.instant = instant;
	}
}
