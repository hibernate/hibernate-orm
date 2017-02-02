/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cut;


/**
 * @author Rob.Hasselbaum
 *
 */
public class MutualFund {
	
	private Long id;
	private MonetoryAmount holdings;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public MonetoryAmount getHoldings() {
		return holdings;
	}

	public void setHoldings(MonetoryAmount holdings) {
		this.holdings = holdings;
	}

}
