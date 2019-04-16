package org.hibernate.test.instrument.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity( name = "DebitCardPayment" )
@Table( name = "debit_payment" )
public class DebitCardPayment extends Payment {
	private String transactionId;

	public DebitCardPayment() {
	}

	public DebitCardPayment(Integer oid, Float amount, String transactionId) {
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
