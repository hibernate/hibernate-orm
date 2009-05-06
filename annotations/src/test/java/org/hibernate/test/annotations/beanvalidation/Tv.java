package org.hibernate.test.annotations.beanvalidation;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Embeddable;
import javax.validation.constraints.Future;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;
import javax.validation.Valid;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Tv {

	@Id
	@Size(max = 2)
	public String serial;
	public int size;
	@Size(max = 2)
	public String name;
	@Future
	public Date expDate;
	@Size(min = 0)
	public String description;
	@Min(1000)
	public BigInteger lifetime;
	@NotNull @Valid
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
		public BigDecimal time;
	}

}
