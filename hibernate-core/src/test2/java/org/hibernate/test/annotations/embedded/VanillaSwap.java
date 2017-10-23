/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Represents an Interest Rate Swap.
 */
@Entity
public class VanillaSwap {

	/**
	 * Possible values for the currency field.
	 */
	public enum Currency {
		USD, GBP, EUR, JPY }

	/**
	 * Identifier of the Interest Rate Swap
	 */
	private String instrumentId;

	/**
	 * Currency of the swap (and of both legs).
	 */
	private Currency currency;

	/**
	 * Fixed leg (cash flows with the fixed rate).
	 */
	private FixedLeg fixedLeg;

	/**
	 * Floating leg (cash flows bound to a financial index).
	 */
	private FloatLeg floatLeg;

	@Embedded
	@AttributeOverride(name = "paymentFrequency", column = @Column(name = "FIXED_FREQENCY"))
	public FixedLeg getFixedLeg() {
		return fixedLeg;
	}

	public void setFixedLeg(FixedLeg fixedLeg) {
		this.fixedLeg = fixedLeg;
	}

	@Embedded
	@AttributeOverride(name = "paymentFrequency", column = @Column(name = "FLOAT_FREQUENCY"))
	public FloatLeg getFloatLeg() {
		return floatLeg;
	}

	public void setFloatLeg(FloatLeg floatLeg) {
		this.floatLeg = floatLeg;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	@Id
	public String getInstrumentId() {
		return instrumentId;
	}

	public void setInstrumentId(String instrumentId) {
		this.instrumentId = instrumentId;
	}
}
