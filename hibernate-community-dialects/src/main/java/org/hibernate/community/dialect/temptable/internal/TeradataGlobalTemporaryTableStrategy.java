/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;

/**
 * Teradata specific global temporary table strategy.
 *
 * @author Steve Ebersole
 */
public class TeradataGlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {

	public static final TeradataGlobalTemporaryTableStrategy INSTANCE = new TeradataGlobalTemporaryTableStrategy();

	@Override
	public String getTemporaryTableCreateOptions() {
		return "on commit preserve rows";
	}

}
