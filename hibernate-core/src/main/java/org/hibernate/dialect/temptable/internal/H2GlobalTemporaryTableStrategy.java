/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;

/**
 * H2 specific global temporary table strategy.
 *
 * @author Steve Ebersole
 */
public class H2GlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {

	public static final H2GlobalTemporaryTableStrategy INSTANCE = new H2GlobalTemporaryTableStrategy();

	@Override
	public String getTemporaryTableCreateOptions() {
		return "transactional";
	}
}
