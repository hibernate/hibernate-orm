/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@Entity
public class Tv {

	@Id
	@Size(max = 2)
	public String serial;

	@Length(max = 5)
	public String model;

	public int size;

	@Size(max = 2)
	public String name;

	@Future
	public Date expDate;

	@Size(min = 0)
	public String description;

	@Min(1000)
	public BigInteger lifetime;

	@NotNull
	@Valid
	public Tuner tuner;

	@Valid
	public Recorder recorder;

	@Embeddable
	public static class Tuner {
		@NotNull
		public String frequency;
	}

	@Embeddable
	public static class Recorder {
		@NotNull
		@Column(name = "`time`")
		public BigDecimal time;
	}
}
