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
public class D {
	@Id
	public int oid;

	@NaturalId(mutable=true)
	public String name;

	@NaturalId(mutable=true)
	@ManyToOne
	public C associatedC;

	public D() {
	}

	public D(int oid, String name, C associatedC) {
		this.oid = oid;
		this.name = name;
		this.associatedC = associatedC;
	}
}
