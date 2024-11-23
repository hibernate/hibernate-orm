/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ExclusiveDependent {
	@EmbeddedId
	DependentId id;

	@JoinColumn(name = "FK", nullable = false)
	// id attribute mapped by join column default
	@MapsId("empPK")
	// maps empPK attribute of embedded id
	@OneToOne
	Employee emp;
}
