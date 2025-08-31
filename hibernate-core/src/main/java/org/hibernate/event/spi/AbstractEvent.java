/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.io.Serializable;

/**
 * Base class for events which are generated from a {@link org.hibernate.Session}
 * or {@linkplain org.hibernate.StatelessSession}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEvent implements Serializable {
	protected final SharedSessionContractImplementor source;

	public AbstractEvent(SharedSessionContractImplementor source) {
		this.source = source;
	}

	public SharedSessionContractImplementor getSession() {
		return source;
	}

	public SessionFactoryImplementor getFactory() {
		return source.getFactory();
	}
}
