/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e2.b;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dependent {
	@EmbeddedId
	DependentId id;

	@MapsId("empPK")

	@ManyToOne
	@JoinColumns( { @JoinColumn(nullable = false), @JoinColumn(nullable = false) })
	Employee emp;
}
