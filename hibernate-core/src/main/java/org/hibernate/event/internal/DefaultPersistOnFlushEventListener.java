/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.event.spi.PersistContext;
import jakarta.annotation.Nonnull;

/**
 * When persist is used as the cascade action, persistOnFlush should be used
 * @author Emmanuel Bernard
 */
public class DefaultPersistOnFlushEventListener extends DefaultPersistEventListener {
	@Override
	protected @Nonnull CascadingAction<PersistContext> getCascadeAction() {
		return CascadingActions.PERSIST_ON_FLUSH;
	}
}
