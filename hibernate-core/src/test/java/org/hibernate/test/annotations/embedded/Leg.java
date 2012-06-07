package org.hibernate.test.annotations.embedded;
import javax.persistence.MappedSuperclass;

/**
 * Represents a leg of a vanilla interest rate swap.
 */
@MappedSuperclass
public class Leg {
	/**
	 * Possible values of the payment frequency field.
	 */
	public enum Frequency {
		ANNUALY, SEMIANNUALLY, QUARTERLY, MONTHLY }

	;

	/**
	 * Shows how frequent payments according to this leg should be made.
	 */
	private Frequency paymentFrequency;

	public Frequency getPaymentFrequency() {
		return paymentFrequency;
	}

	public void setPaymentFrequency(Frequency paymentFrequency) {
		this.paymentFrequency = paymentFrequency;
	}

}
