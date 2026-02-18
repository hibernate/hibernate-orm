/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.generationmappings;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.Optimizer;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;

@Entity
public class OptimizerSequenceEntity2 {

	private Long id;

	@Id
	@GeneratedValue(generator="sequence_gen")
	@SequenceGenerator(name="sequence_gen", sequenceName = "optimizer_sequence2", allocationSize = 30)
	@Optimizer(StandardOptimizerDescriptor.POOLED_LOTL)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
