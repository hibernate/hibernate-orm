/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
public class RangeChecks {

	@Id
	@GeneratedValue
	private Long id;

	@Min(5)
	private int minOnly;

	@Max(100)
	private int maxOnly;

	@DecimalMin("1.5")
	private BigDecimal decimalMinInclusive;

	@DecimalMin(value = "1.5", inclusive = false)
	private BigDecimal decimalMinExclusive;

	@DecimalMax("99.5")
	private BigDecimal decimalMaxInclusive;

	@DecimalMax(value = "99.5", inclusive = false)
	private BigDecimal decimalMaxExclusive;

	@Positive
	private int positive;

	@PositiveOrZero
	private int positiveOrZero;

	@Negative
	private int negative;

	@NegativeOrZero
	private int negativeOrZero;

	@Min(5)
	@Positive
	private int minAndPositive;

	@Max(-5)
	@Negative
	private int maxAndNegative;

	@DecimalMin("3.0")
	@PositiveOrZero
	private BigDecimal decimalMinAndPositiveOrZero;

	@CustomMinAndPositive
	private int composedAnd;

	@CustomMinOrMax
	private int composedOr;

	@Min(3)
	@CustomMinOrPositiveOrZero
	private int composedOrSingleSideMerged;
}
