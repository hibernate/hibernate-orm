/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences.entities;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Sample of class generator
 *
 * @author Emmanuel Bernard
 */
@Entity
@jakarta.persistence.SequenceGenerator(
		name = "SEQ_STORE",
		sequenceName = "my_sequence"
)
@SuppressWarnings("serial")
public class Store implements Serializable {
	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STORE")
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}
}
