/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance.cache;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Cacheable
@NaturalIdCache
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class MyEntity {
	private Integer id;
	private String uid;

	protected MyEntity() {
	}

	public MyEntity(Integer id, String uid) {
		this.id = id;
		this.uid = uid;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@NaturalId
	@Column(name = "natural_id")
	public String getUid() {
		return uid;
	}

	public void setUid(final String uid) {
		this.uid = uid;
	}
}
