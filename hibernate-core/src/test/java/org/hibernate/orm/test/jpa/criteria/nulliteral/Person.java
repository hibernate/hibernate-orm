/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.nulliteral;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Andrea Boriero
 */
@Entity
public class Person {
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	private Subject subject;

	@Enumerated
	private EyeColor eyeColor;
}
