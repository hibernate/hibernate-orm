package org.hibernate.test.annotations.beanvalidation;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PowerSupply {
	private Integer id;
	private BigDecimal power;
	private String position;

	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Min(100) @Max(250)
	public BigDecimal getPower() {
		return power;
	}

	public void setPower(BigDecimal power) {
		this.power = power;
	}

	@Column(name="fld_pos")
	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}
}
