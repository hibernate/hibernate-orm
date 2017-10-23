/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

/**
 * Interest Rate Swap with Tenor. Used here to compose
 * a swap spread deal.
 */
@Embeddable
public class Swap {

	/**
	 * Tenor (duration) of the swap (in years).
	 */
	private int tenor;

	public int getTenor() {
		return tenor;
	}

	public void setTenor(int tenor) {
		this.tenor = tenor;
	}

	/**
	 * Fixed leg (cash flows with the fixed rate).
	 */
	private FixedLeg fixedLeg;

	/**
	 * Floating leg (cash flows bound to a financial index).
	 */
	private FloatLeg floatLeg;

	@Embedded
	// We retain this annotation to test the precedence of @AttributeOverride
	// Outermost override annotation should win
	@AttributeOverride(name = "paymentFrequency", column = @Column(name = "FIXED_FREQENCY"))
	public FixedLeg getFixedLeg() {
		return fixedLeg;
	}

	public void setFixedLeg(FixedLeg fixedLeg) {
		this.fixedLeg = fixedLeg;
	}

	@Embedded
	public FloatLeg getFloatLeg() {
		return floatLeg;
	}

	public void setFloatLeg(FloatLeg floatLeg) {
		this.floatLeg = floatLeg;
	}
}
