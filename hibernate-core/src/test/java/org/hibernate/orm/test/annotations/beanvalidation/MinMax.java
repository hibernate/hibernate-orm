/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class MinMax {

	@Id
	@GeneratedValue
	private Long id;

	@Max(10)
	@Min(2)
	@Column(name = "`value`")
	private Integer value;

	private MinMax() {
	}

	public MinMax(Integer value) {
		this.value = value;
	}
}
