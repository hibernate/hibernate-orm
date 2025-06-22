/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.TableGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Invoice {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "number_sequence")
	@GenericGenerator(
			name = "number_sequence",
			strategy = "org.hibernate.id.enhanced.TableGenerator",
			parameters = {
					@Parameter(name = TableGenerator.SEGMENT_VALUE_PARAM, value = "customer"),
					@Parameter(name = TableGenerator.INCREMENT_PARAM, value = "5"),
					@Parameter(name = TableGenerator.OPT_PARAM, value = "pooled")
			}
	)
	private Long id;

	public Long getId() {
		return id;
	}
}
