//$Id$
package org.hibernate.test.annotations.embedded;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@AttributeOverrides(value = {
		@AttributeOverride(name = "swap.tenor", column = @Column(name = "TENOR")), //should be ovvriden by deal
		@AttributeOverride(name = "id", column = @Column(name = "NOTONIALDEAL_ID"))
})
@MappedSuperclass
public class NotonialDeal extends Deal {
	/**
	 * Notional amount of both IRSs.
	 */
	private double notional;

	public double getNotional() {
		return notional;
	}

	public void setNotional(double notional) {
		this.notional = notional;
	}
}
