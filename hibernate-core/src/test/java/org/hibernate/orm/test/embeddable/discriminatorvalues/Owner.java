/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable.discriminatorvalues;


import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "catMothers", query = "select treat(o.pet as Cat).mother from Owner o")
public class Owner {
	@Id
	private Long id;

	@Embedded
	private Animal pet;
}
