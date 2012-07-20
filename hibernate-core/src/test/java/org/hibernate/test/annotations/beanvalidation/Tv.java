/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.test.annotations.beanvalidation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.validation.Valid;
import javax.validation.constraints.Future;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
