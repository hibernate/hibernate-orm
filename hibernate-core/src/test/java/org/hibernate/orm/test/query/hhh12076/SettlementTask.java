/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh12076;

public class SettlementTask extends Task<Settlement> {

	private Settlement _linked;

	public Settlement getLinked() {
		return _linked;
	}

	public void setLinked(Settlement settlement) {
		_linked = settlement;
	}

}
