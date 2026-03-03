/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.fk.ForeignKeyModel;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractGraphBuilder implements GraphBuilder {
	protected final ForeignKeyModel fkModel;
	protected final boolean avoidBreakingDeferrable;
	protected final boolean ignoreDeferrableForOrdering;

	public AbstractGraphBuilder(
			ForeignKeyModel fkModel,
			boolean avoidBreakingDeferrable,
			boolean ignoreDeferrableForOrdering) {
		this.fkModel = fkModel;
		this.avoidBreakingDeferrable = avoidBreakingDeferrable;
		this.ignoreDeferrableForOrdering = ignoreDeferrableForOrdering;
	}

}
