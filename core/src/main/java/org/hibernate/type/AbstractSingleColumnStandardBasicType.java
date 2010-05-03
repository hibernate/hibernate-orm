/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSingleColumnStandardBasicType<T>
		extends AbstractStandardBasicType<T>
		implements SingleColumnType<T> {

	public AbstractSingleColumnStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		super( sqlTypeDescriptor, javaTypeDescriptor );
	}

	private static WrapperOptions NO_OPTIONS = new WrapperOptions() {
		public boolean useStreamForLobBinding() {
			return false;
		}

		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}
	};

	public final int sqlType() {
		return getSqlTypeDescriptor().getSqlType();
	}

	/**
	 * {@inheritDoc}
	 */
	public T nullSafeGet(ResultSet rs, String name) throws HibernateException, SQLException {
		return nullSafeGet( rs, name, NO_OPTIONS );
	}

	/**
	 * {@inheritDoc}
	 */
	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
		return nullSafeGet( rs, name );
	}

	/**
	 * {@inheritDoc}
	 */
	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void nullSafeSet(PreparedStatement st, T value, int index) throws HibernateException, SQLException {
		nullSafeSet( st, value, index, NO_OPTIONS );
	}

	/**
	 * {@inheritDoc}
	 */
	public void set(PreparedStatement st, T value, int index) throws HibernateException, SQLException {
		nullSafeSet( st, value, index );
	}

}