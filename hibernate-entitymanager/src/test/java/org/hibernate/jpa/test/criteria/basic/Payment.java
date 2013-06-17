package org.hibernate.jpa.test.criteria.basic;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/**
 * @author Francois Gerodez
 */
@Entity
@Table(name = "crit_basic_payment")
@TypeDef(name = "paymentDate", typeClass = Date3Type.class)
public class Payment {

	private Long id;
	private BigDecimal amount;
	private Date date;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Type(type = "paymentDate")
	@Columns(columns = { @Column(name = "YEARPAYMENT"), @Column(name = "MONTHPAYMENT"), @Column(name = "DAYPAYMENT") })
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
