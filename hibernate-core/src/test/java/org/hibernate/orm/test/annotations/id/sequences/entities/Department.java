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
 * Sample of method generator
 *
 * @author Emmanuel Bernard
 */
@Entity
@SuppressWarnings("serial")
public class Department implements Serializable {
	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DEPT")
	@jakarta.persistence.SequenceGenerator(
			name = "SEQ_DEPT",
			sequenceName = "my_sequence"
	)
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}
}
