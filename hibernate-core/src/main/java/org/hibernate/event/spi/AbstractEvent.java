/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.io.Serializable;
import jakarta.annotation.Nonnull;

/**
 * Base class for events which are generated from a {@link org.hibernate.Session}
 * or {@linkplain org.hibernate.StatelessSession}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEvent implements Serializable {
	protected final SharedSessionContractImplementor source;

	public AbstractEvent(@Nonnull SharedSessionContractImplementor source) {
		this.source = source;
	}

	@Nonnull
	public SharedSessionContractImplementor getSession() {
		return source;
	}

	@Nonnull
	public SessionFactoryImplementor getFactory() {
		return source.getFactory();
	}
}
