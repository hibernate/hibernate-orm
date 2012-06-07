package org.hibernate.test.annotations.embedded;
import java.text.NumberFormat;
import javax.persistence.Embeddable;

/**
 * Represents floating part of Interest Rate Swap cash flows.
 */
@Embeddable
public class FloatLeg extends Leg {

	/**
	 * Possible values for the rate index.
	 */
	public enum RateIndex {
		LIBOR, EURIBOR, TIBOR}

	;

	private RateIndex rateIndex;

	/**
	 * Spread over the selected rate index (in basis points).
	 */
	private double rateSpread;

	public RateIndex getRateIndex() {
		return rateIndex;
	}

	public void setRateIndex(RateIndex rateIndex) {
		this.rateIndex = rateIndex;
	}

	public double getRateSpread() {
		return rateSpread;
	}

	public void setRateSpread(double rateSpread) {
		this.rateSpread = rateSpread;
	}

	public String toString() {
		NumberFormat format = NumberFormat.getNumberInstance();
		format.setMinimumFractionDigits( 1 );
		format.setMaximumFractionDigits( 1 );
		return "[" + getRateIndex().toString() + "+" + format.format( getRateSpread() ) + "]";
	}
}
