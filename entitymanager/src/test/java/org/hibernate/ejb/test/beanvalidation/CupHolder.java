package org.hibernate.ejb.test.beanvalidation;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class CupHolder {
	@Id
	@GeneratedValue
	//@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Integer id;
	private BigDecimal radius;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Max( value = 10, message = "Radius way out")
	@NotNull
	public BigDecimal getRadius() {
		return radius;
	}

	public void setRadius(BigDecimal radius) {
		this.radius = radius;
	}
}
