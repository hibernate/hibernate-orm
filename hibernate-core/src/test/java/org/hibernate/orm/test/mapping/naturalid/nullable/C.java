/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.NaturalId;

/**
 * @author Guenther Demetz
 */
@Entity
public class C {
	@Id
	public int oid;

	@NaturalId(mutable=true)
	public String name;

	public C() {
	}

	public C(int oid, String name) {
		this.oid = oid;
		this.name = name;
	}
}
