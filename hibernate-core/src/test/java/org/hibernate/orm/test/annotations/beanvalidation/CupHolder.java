/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;
import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class CupHolder {
	@Id
	@GeneratedValue
	private Integer id;
	private BigDecimal radius;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Max(value = 10, message = "Radius way out")
	@NotNull(groups = Strict.class)
	public BigDecimal getRadius() {
		return radius;
	}

	public void setRadius(BigDecimal radius) {
		this.radius = radius;
	}
}
