/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.aggregate.spi.StandardAggregateSupport;
import org.hibernate.type.SqlTypes;

/// Provider-owned aggregate extension based only on the supported standard
/// profile.
///
/// @author Steve Ebersole
/// @since 8.0
public final class ExampleAggregateSupport extends StandardAggregateSupport {
	static final ExampleAggregateSupport INSTANCE = new ExampleAggregateSupport();

	private ExampleAggregateSupport() {
	}

	@Override
	public boolean preferSelectAggregateMapping(int aggregateSqlTypeCode) {
		return aggregateSqlTypeCode == SqlTypes.JSON
				? false
				: super.preferSelectAggregateMapping( aggregateSqlTypeCode );
	}
}
