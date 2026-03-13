/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.generationmappings;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.GeneratorOptimizer;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;

@Entity
public class OptimizerSequenceEntity {

	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@SequenceGenerator(sequenceName = "optimizer_sequence", allocationSize = 20)
	@GeneratorOptimizer(StandardOptimizerDescriptor.POOLED_LO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
