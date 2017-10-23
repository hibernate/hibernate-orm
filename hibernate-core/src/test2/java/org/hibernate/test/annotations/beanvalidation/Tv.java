/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
