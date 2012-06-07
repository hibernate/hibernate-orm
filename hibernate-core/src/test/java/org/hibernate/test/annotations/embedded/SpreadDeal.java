package org.hibernate.test.annotations.embedded;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;

/**
 * Represents a "curve spread" deal that consists of
 * two Interest Rate Swaps with different tenors (short and long).
 * For simplicity, tenors are not considered here.
 */
@Entity
@AttributeOverrides(value = {
		@AttributeOverride(name = "swap.tenor", column = @Column(name = "MEDIUM_TENOR")),
		@AttributeOverride(name = "swap.fixedLeg.paymentFrequency", column = @Column(name = "MEDIUM_FIXED_FREQUENCY")),
		@AttributeOverride(name = "swap.fixedLeg.rate", column = @Column(name = "MEDIUM_FIXED_RATE")),
		@AttributeOverride(name = "swap.floatLeg.paymentFrequency", column = @Column(name = "MEDIUM_FLOAT_FREQUENCY")),
		@AttributeOverride(name = "swap.floatLeg.rateIndex", column = @Column(name = "MEDIUM_FLOAT_RATEINDEX")),
		@AttributeOverride(name = "swap.floatLeg.rateSpread", column = @Column(name = "MEDIUM_FLOAT_RATESPREAD"))
})
public class SpreadDeal extends NotonialDeal {

	/**
	 * Swap with the tenor.
	 */
	private Swap longSwap;

	@Embedded
	public Swap getLongSwap() {
		return longSwap;
	}

	public void setLongSwap(Swap swap) {
		this.longSwap = swap;
	}


	/**
	 * Swap with the longer tenor.
	 */
	private Swap shortSwap;

	@Embedded
	@AttributeOverrides(value = {
			@AttributeOverride(name = "tenor", column = @Column(name = "SHORT_TENOR")),
			@AttributeOverride(name = "fixedLeg.paymentFrequency", column = @Column(name = "SHORT_FIXED_FREQUENCY")),
			@AttributeOverride(name = "fixedLeg.rate", column = @Column(name = "SHORT_FIXED_RATE")),
			@AttributeOverride(name = "floatLeg.paymentFrequency", column = @Column(name = "SHORT_FLOAT_FREQUENCY")),
			@AttributeOverride(name = "floatLeg.rateIndex", column = @Column(name = "SHORT_FLOAT_RATEINDEX")),
			@AttributeOverride(name = "floatLeg.rateSpread", column = @Column(name = "SHORT_FLOAT_RATESPREAD"))
	})
	public Swap getShortSwap() {
		return shortSwap;
	}

	public void setShortSwap(Swap shortSwap) {
		this.shortSwap = shortSwap;
	}
}
