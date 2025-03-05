/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
@Access(AccessType.FIELD)
public class BaseEntity {
	@Id
	@SequenceGenerator(name = "test1_id_gen", sequenceName = "test1_seq")
	@GeneratedValue(generator = "test1_id_gen", strategy = GenerationType.SEQUENCE)
	protected Integer id;

	protected String name;

	public BaseEntity() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
