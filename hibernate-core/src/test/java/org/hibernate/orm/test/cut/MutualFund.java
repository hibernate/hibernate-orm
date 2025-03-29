/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;


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
