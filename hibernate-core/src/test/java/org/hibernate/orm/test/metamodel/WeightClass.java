/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * @author Marco Belladelli
 */
@Entity
@IdClass(Weight.class)
public class WeightClass {
	@Id
	private String unit;

	@Id
	private float weight;

	private String description;
}
