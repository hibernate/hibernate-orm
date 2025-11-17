/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.joined2;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "dogs")
@PrimaryKeyJoinColumn(name = "id_dog", referencedColumnName = "id_animal")
public class Dog extends Animal {
	private String breed;
	@ManyToOne @JoinColumn(name = "id_owner")
	private Owner owner;
}
