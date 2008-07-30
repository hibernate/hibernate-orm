/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.type;

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;

/**
 * Superclass for mutable nullable types
 * @author Gavin King
 */
public abstract class MutableType extends NullableType {

	public final boolean isMutable() {
		return true;
	}

	protected abstract Object deepCopyNotNull(Object value) throws HibernateException;

	public final Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) 
	throws HibernateException {
		return (value==null) ? null : deepCopyNotNull(value);
	}

	public Object replace(
		Object original,
		Object target,
		SessionImplementor session,
		Object owner, 
		Map copyCache)
	throws HibernateException {
		if ( isEqual( original, target, session.getEntityMode() ) ) return original;
		return deepCopy( original, session.getEntityMode(), session.getFactory() );
	}

}
