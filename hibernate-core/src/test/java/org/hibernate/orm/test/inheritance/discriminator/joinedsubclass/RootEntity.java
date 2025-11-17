/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Andrea Boriero
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn()
@DiscriminatorValue("ROOT")
public class RootEntity {
	@Id
	@GeneratedValue
	private Long id;

	public Long getId() {
		return id;
	}
}
