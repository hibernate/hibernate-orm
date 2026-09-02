/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;

/// Global temporary-table behavior for InterSystems IRIS.
///
/// @since 8.0
/// @author Steve Ebersole
public final class InterSystemsIRISGlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {
	public static final InterSystemsIRISGlobalTemporaryTableStrategy INSTANCE =
			new InterSystemsIRISGlobalTemporaryTableStrategy();

	private InterSystemsIRISGlobalTemporaryTableStrategy() {
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return null;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create global temporary table if not exists";
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}
}
