/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy;

import java.io.Serializable;

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
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		return doImmediateLoad( entityName );
	}
}
