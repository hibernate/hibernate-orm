/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptionsAdapter;

public class LimitQueryOptions extends QueryOptionsAdapter {

	private final Limit limit;

	public LimitQueryOptions(Limit limit) {
		this.limit = limit;
	}

	@Override
	public Limit getLimit() {
		return limit;
	}
}
