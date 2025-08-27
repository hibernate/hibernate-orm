/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * @author Guenther Demetz
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class D {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	public long oid;

	@Version
	private int version;

	@jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
	private A a = null;

	@jakarta.persistence.OneToOne(mappedBy = "singleD")
	private A singleA = null;

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}

	public A getSingleA() {
		return singleA;
	}

	public void setSingleA(A singleA) {
		this.singleA = singleA;
	}
}
