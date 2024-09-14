/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

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
