/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractDelegateSessionImplementor extends SessionDelegatorBaseImpl implements SessionImplementor {
	public AbstractDelegateSessionImplementor(SessionImplementor delegate) {
		super( delegate );
	}

	public abstract Object doImmediateLoad(String entityName);

	@Override
	public Object immediateLoad(String entityName, Object id) throws HibernateException {
		return doImmediateLoad( entityName );
	}
}
