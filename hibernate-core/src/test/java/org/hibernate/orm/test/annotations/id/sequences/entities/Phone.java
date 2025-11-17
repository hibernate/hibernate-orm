/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences.entities;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity()
public class Phone {
	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Phone_Gen")
	@jakarta.persistence.SequenceGenerator(
			name = "Phone_Gen",
			sequenceName = "phone_seq"
	)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
