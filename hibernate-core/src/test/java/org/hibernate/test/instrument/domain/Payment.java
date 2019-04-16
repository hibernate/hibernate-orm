package org.hibernate.test.instrument.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity( name = "Payment" )
@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
public abstract class Payment {
	private Integer oid;
	private Float amount;

	public Payment() {
	}

	public Payment(Integer oid, Float amount) {
		this.oid = oid;
		this.amount = amount;
	}

	@Id
	@Column( name = "oid" )
	public Integer getOid() {
		return oid;
	}

	public void setOid(Integer oid) {
		this.oid = oid;
	}

	public Float getAmount() {
		return amount;
	}

	public void setAmount(Float amount) {
		this.amount = amount;
	}
}
