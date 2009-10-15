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

import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Hibernate;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.WrappedBlob;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.util.ArrayHelper;

/**
 * <tt>blob</tt>: A type that maps an SQL BLOB to a java.sql.Blob.
 * @author Gavin King
 */
public class BlobType extends AbstractType {
	/**
	 * {@inheritDoc}
	 */
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SessionImplementor session) throws HibernateException, SQLException {
		if ( settable[0] ) {
			set( st, value, index, session );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SessionImplementor session) throws HibernateException, SQLException {
		set( st, value, index, session );
	}

	public void set(
			PreparedStatement st,
			Object value,
			int index,
			SessionImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, Types.BLOB );
			return;
		}

		Blob blob = ( Blob ) value;

		if ( WrappedBlob.class.isInstance( blob ) ) {
			blob = ( (WrappedBlob) value ).getWrappedBlob();
		}

		final boolean useInputStream = session.getFactory().getDialect().useInputStreamToInsertBlob()
				&& BlobImplementer.class.isInstance( blob );

		if ( useInputStream ) {
			st.setBinaryStream( index, blob.getBinaryStream(), (int) blob.length() );
		}
		else {
			st.setBlob( index, blob );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Object nullSafeGet(
			ResultSet rs,
			String name,
			SessionImplementor session,
			Object owner) throws HibernateException, SQLException {
		return get( rs, name, Hibernate.getLobCreator( session ) );
	}

	/**
	 * {@inheritDoc}
	 */
	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws HibernateException, SQLException {
		return get( rs, names[0], Hibernate.getLobCreator( session ) );
	}

	/**
	 * A method to extract the BLOB value from a result set.
	 *
	 * @param rs The result set
	 * @param name The name of the column containing the BLOB
	 *
	 * @return The BLOB
	 *
	 * @throws SQLException Indicates a problem accessing the result set
	 *
	 * @deprecated Use {@link #get(ResultSet,String,LobCreator)} instead
	 */
	public Object get(ResultSet rs, String name) throws SQLException {
		return get( rs, name, NonContextualLobCreator.INSTANCE );
	}

	public Object get(ResultSet rs, String name, LobCreator lobCreator) throws SQLException {
		Blob value = rs.getBlob( name );
		return rs.wasNull() ? null : lobCreator.wrap( value );
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getReturnedClass() {
		return Blob.class;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return x == y;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getHashCode(Object x, EntityMode entityMode) {
		return System.identityHashCode(x);
	}

	/**
	 * {@inheritDoc}
	 */
	public int compare(Object x, Object y, EntityMode entityMode) {
		return 0; //lobs cannot be compared
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "blob";
	}

	/**
	 * {@inheritDoc}
	 */
	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
		throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Blobs are not cacheable");
	}

	/**
	 * {@inheritDoc}
	 */
	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)  {
		return value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object fromXMLNode(Node xml, Mapping factory) {
		throw new UnsupportedOperationException("todo");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getColumnSpan(Mapping mapping) {
		return 1;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isMutable() {
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache) throws HibernateException {
		//Blobs are ignored by merge()
		return target;
	}

	/**
	 * {@inheritDoc}
	 */
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return new int[] { Types.BLOB };
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException("todo");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return value==null ? "null" : value.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value==null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDirty(
			Object old,
			Object current,
			boolean[] checkable,
			SessionImplementor session) throws HibernateException {
		return checkable[0] && isDirty(old, current, session);
	}

}

