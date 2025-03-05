/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.joined2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "owners")
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = "companyFilter", condition = "id_company = :companyIdParam")
public class Owner {
	@Id
	@Column(name = "id_owner")
	private int id;

	private String name;

	@ManyToOne
	@JoinColumn(name = "id_dog")
	private Dog dog;

	@Column(name = "id_company")
	private long company;
}
