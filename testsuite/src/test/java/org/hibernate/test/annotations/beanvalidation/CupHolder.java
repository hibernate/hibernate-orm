package org.hibernate.test.annotations.beanvalidation;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

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

	@Max( value = 10, message = "Radius way out")
	@NotNull(groups = Strict.class)
	public BigDecimal getRadius() {
		return radius;
	}

	public void setRadius(BigDecimal radius) {
		this.radius = radius;
	}
}
