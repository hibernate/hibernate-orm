package org.hibernate.test.instrument.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity( name = "CreditCardPayment" )
@Table( name = "credit_payment" )
public class CreditCardPayment extends Payment {
	private String transactionId;

	public CreditCardPayment() {
	}

	public CreditCardPayment(Integer oid, Float amount, String transactionId) {
		super(oid, amount);
		this.transactionId = transactionId;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
}
