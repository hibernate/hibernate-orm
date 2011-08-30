package org.hibernate.test.annotations.embedded;
import java.text.NumberFormat;
import javax.persistence.Embeddable;

/**
 * Represents fixed part of Interest Rate Swap cash flows.
 */
@Embeddable
public class FixedLeg extends Leg {

	/**
	 * Fixed rate.
	 */
	private double rate;

	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public String toString() {
		NumberFormat format = NumberFormat.getNumberInstance();
		format.setMinimumFractionDigits( 4 );
		format.setMaximumFractionDigits( 4 );
		return format.format( getRate() ) + "%";
	}
}
