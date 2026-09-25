package org.hibernate.orm.test.bidi;

/**
 */
public class SpecialBid extends AbstractBid {
	private boolean isSpecial;

	public boolean isSpecial() {
		return isSpecial;
	}

	public void setSpecial(boolean isSpecial) {
		this.isSpecial = isSpecial;
	}
}
