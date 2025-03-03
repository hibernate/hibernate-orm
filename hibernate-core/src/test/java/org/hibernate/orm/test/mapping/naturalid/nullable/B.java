/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;

/**
 * @author Guenther Demetz
 */
@Entity
public class B {
	@Id
	public int oid;

	@ManyToOne
	@NaturalId(mutable = true)
	public A assA = null;

	@NaturalId(mutable = true)
	public int naturalid;

	public B() {
	}

	public B(int oid, A assA, int naturalid) {
		this.oid = oid;
		this.assA = assA;
		this.naturalid = naturalid;
	}
}
