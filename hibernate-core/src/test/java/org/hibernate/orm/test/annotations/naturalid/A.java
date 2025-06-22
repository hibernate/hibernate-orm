/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

/**
 * @author Guenther Demetz
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NaturalIdCache
public class A {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long oid;

	@Version
	private int version;

	@Column
	@NaturalId(mutable = false)
	private String name;

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@org.hibernate.annotations.OptimisticLock(excluded = true)
	@jakarta.persistence.OneToMany(mappedBy = "a")
	private Set<D> ds = new HashSet<D>();

	@jakarta.persistence.OneToOne
	private D singleD = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<D> getDs() {
		return ds;
	}

	public void setDs(Set<D> ds) {
		this.ds = ds;
	}

	public D getSingleD() {
		return singleD;
	}

	public void setSingleD(D singleD) {
		this.singleD = singleD;
	}

}
