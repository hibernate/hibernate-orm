/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import jakarta.annotation.Nonnull;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptionsAdapter;

public class LimitQueryOptions extends QueryOptionsAdapter {

	private final Limit limit;

	public LimitQueryOptions(@Nonnull Limit limit) {
		this.limit = limit;
	}

	@Override
	@Nonnull
	public Limit getLimit() {
		return limit;
	}
}
