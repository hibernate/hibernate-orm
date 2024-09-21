/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import static jakarta.persistence.DiscriminatorType.INTEGER;
import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;

/**
 * @author Pawel Stawicki
 */
@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "CLASS_ID", discriminatorType = INTEGER)
public abstract class ParentEntity  {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "ajdik")
	private Long id;

	public Long getId() {
		return id;
	}
}
