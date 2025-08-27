/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.sequencegenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = SequenceGeneratorOptionsTest.TABLE_NAME)
public class TestEntity {
	@Id
	@SequenceGenerator(name = "seq_gen", sequenceName = SequenceGeneratorOptionsTest.SEQUENCE_GENERATOR_NAME, options = SequenceGeneratorOptionsTest.SEQUENCE_GENERATOR_OPTIONS)
	@GeneratedValue(generator = "seq_gen", strategy = GenerationType.SEQUENCE)
	private Long id;

	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
