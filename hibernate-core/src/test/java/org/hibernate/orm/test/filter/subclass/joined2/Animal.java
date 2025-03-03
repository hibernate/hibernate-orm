/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.joined2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import static jakarta.persistence.InheritanceType.JOINED;

@Entity
@Table(name = "animals")
@Inheritance(strategy = JOINED)
@Filter(name = "companyFilter", condition = "id_company = :companyIdParam")
public class Animal {
	@Id
	@Column(name = "id_animal")
	private int id;

	private String name;

	@Column(name = "id_company")
	private long company;
}
