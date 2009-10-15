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
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.jdbc.WrappedClob;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.util.ArrayHelper;

/**
 * <tt>clob</tt>: A type that maps an SQL CLOB to a java.sql.Clob.
 * @author Gavin King
 */
public class ClobType extends AbstractType {

	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SessionImplementor session) throws SQLException {
		if ( settable[0] ) {
			set( st, value, index, session );
		}
	}

	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SessionImplementor session) throws SQLException {
		set( st, value, index, session );
	}

	public void set(
			PreparedStatement st,
			Object value,
			int index,
			SessionImplementor session) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.CLOB );
			return;
		}

		Clob clob = ( Clob ) value;

		if ( WrappedClob.class.isInstance( clob ) ) {
			clob = ( (WrappedClob) value ).getWrappedClob();
		}

		final boolean useInputStream = session.getFactory().getDialect().useInputStreamToInsertBlob()
				&& ClobImplementer.class.isInstance( clob );

		if ( useInputStream ) {
			st.setCharacterStream( index, clob.getCharacterStream(), (int) clob.length() );
		}
		else {
			st.setClob( index, clob );
		}
	}

	public Object nullSafeGet(
			ResultSet rs,
			String name,
			SessionImplementor session,
			Object owner) throws SQLException {
		return get( rs, name, Hibernate.getLobCreator( session ) );
	}

	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws SQLException {
		return get( rs, names[0], Hibernate.getLobCreator( session ) );
	}

	/**
	 * A method to extract the CLOB value from a result set.
	 *
	 * @param rs The result set
	 * @param name The name of the column containing the CLOB
	 *
	 * @return The CLOB
	 *
	 * @throws SQLException Indicates a problem accessing the result set
	 *
	 * @deprecated Use {@link #get(ResultSet,String,LobCreator)} instead
	 */
	public Object get(ResultSet rs, String name) throws SQLException {
		return get( rs, name, NonContextualLobCreator.INSTANCE );
	}

	public Clob get(ResultSet rs, String name, LobCreator lobCreator) throws SQLException {
		Clob value = rs.getClob( name );
		return rs.wasNull() ? null : lobCreator.wrap( value );
	}

	public Class getReturnedClass() {
		return Clob.class;
	}

	public boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return x == y;
	}
	
	public int getHashCode(Object x, EntityMode entityMode) {
		return System.identityHashCode(x);
	}

	public int compare(Object x, Object y, EntityMode entityMode) {
		return 0; //lobs cannot be compared
	}

	public String getName() {
		return "clob";
	}
	
	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
		throws HibernateException {
		throw new UnsupportedOperationException("Clobs are not cacheable");
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)  {
		return value;
	}
	
	public Object fromXMLNode(Node xml, Mapping factory) {
		return Hibernate.createClob( xml.getText() );
	}
	
	public int getColumnSpan(Mapping mapping) {
		return 1;
	}
	
	public boolean isMutable() {
		return false;
	}
	
	public Object replace(Object original, Object target,
			SessionImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		//Clobs are ignored by merge() operation
		return target;
	}
	
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return new int[] { Types.CLOB };
	}
	
	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) {
		if (value!=null) {
			Clob clob = (Clob) value;
			try {
				int len = (int) clob.length();
				node.setText( clob.getSubString(0, len) );
			}
			catch (SQLException sqle) {
				throw new HibernateException("could not read XML from Clob", sqle);
			}
		}
	}
	
	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		return value==null ? "null" : value.toString();
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value==null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) throws HibernateException {
		return checkable[0] && isDirty(old, current, session);
	}
	
}






