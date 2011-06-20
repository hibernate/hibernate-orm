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
 */
package org.hibernate.type;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.relational.Size;

/**
 * @author Emmanuel Bernard
 * @deprecated
 */
@Deprecated
public abstract class AbstractLobType extends AbstractType implements Serializable {
	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		return checkable[0] ? ! isEqual( old, current ) : false;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return new Size[] { LEGACY_DICTATED_SIZE };
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return new Size[] { LEGACY_DEFAULT_SIZE };
	}

	@Override
	public boolean isEqual(Object x, Object y) {
		return isEqual( x, y, null );
	}

	@Override
	public int getHashCode(Object x) {
		return getHashCode( x, null );
	}

	public String getName() {
		return this.getClass().getName();
	}

	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	protected abstract Object get(ResultSet rs, String name) throws SQLException;

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return get( rs, names[0] );
	}

	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return get( rs, name );
	}

	public void nullSafeSet(
			PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session
	) throws HibernateException, SQLException {
		if ( settable[0] ) set( st, value, index, session );
	}

	protected abstract void set(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws SQLException;

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws HibernateException, SQLException {
		set( st, value, index, session );
	}
}
