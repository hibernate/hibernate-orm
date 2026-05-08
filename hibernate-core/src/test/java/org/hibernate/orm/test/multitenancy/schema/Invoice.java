/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Invoice {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "number_sequence")
	@TableGenerator(name = "number_sequence", pkColumnValue = "customer", allocationSize = 5)
	private Long id;

	public Long getId() {
		return id;
	}
}
