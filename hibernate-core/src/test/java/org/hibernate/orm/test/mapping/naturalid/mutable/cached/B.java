/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.mutable.cached;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

/**
 * @author Guenther Demetz
 */
@Entity
@NaturalIdCache
public class B {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	public long oid;

	@ManyToOne
	@NaturalId(mutable = true)
	public A assA = null;

	@NaturalId(mutable = true)
	public int naturalid;
}
