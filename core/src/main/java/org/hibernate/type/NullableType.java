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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.dom4j.Node;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.EqualsHelper;
import org.hibernate.util.StringHelper;

/**
 * Superclass of single-column nullable types.
 * 
 * @author Gavin King
 */
public abstract class NullableType extends AbstractType {

	/**
	 * This is the old scheme where logging of parameter bindings and value extractions
	 * was controlled by the trace level enablement on the 'org.hibernate.type' package...
	 * <p/>
	 * Originally was cached such because of performance of looking up the logger each time
	 * in order to check the trace-enablement.  Driving this via a central Log-specific class
	 * would alleviate that performance hit, and yet still allow more "normal" logging usage/config.
	 */
	private static final boolean IS_VALUE_TRACING_ENABLED = LoggerFactory.getLogger( StringHelper.qualifier( Type.class.getName() ) ).isTraceEnabled();
	private transient Logger log;

	private Logger log() {
		if ( log == null ) {
			log = LoggerFactory.getLogger( getClass() );
		}
		return log;
	}

	/**
	 * Get a column value from a result set, without worrying about the
	 * possibility of null values.  Called from {@link #nullSafeGet} after
	 * nullness checks have been performed.
	 *
	 * @param rs The result set from which to extract the value.
	 * @param name The name of the value to extract.
	 *
	 * @return The extracted value.
	 *
	 * @throws org.hibernate.HibernateException Generally some form of mismatch error.
	 * @throws java.sql.SQLException Indicates problem making the JDBC call(s).
	 */
	public abstract Object get(ResultSet rs, String name) throws HibernateException, SQLException;

	/**
	 * Set a parameter value without worrying about the possibility of null
	 * values.  Called from {@link #nullSafeSet} after nullness checks have
	 * been performed.
	 *
	 * @param st The statement into which to bind the parameter value.
	 * @param value The parameter value to bind.
	 * @param index The position or index at which to bind the param value.
	 *
	 * @throws org.hibernate.HibernateException Generally some form of mismatch error.
	 * @throws java.sql.SQLException Indicates problem making the JDBC call(s).
	 */
	public abstract void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException;

	/**
	 * A convenience form of {@link #sqlTypes(org.hibernate.engine.Mapping)}, returning
	 * just a single type value since these are explicitly dealing with single column
	 * mappings.
	 *
	 * @return The {@link java.sql.Types} mapping value.
	 */
	public abstract int sqlType();

	/**
	 * A null-safe version of {@link #toString(Object)}.  Specifically we are
	 * worried about null safeness in regards to the incoming value parameter,
	 * not the return.
	 *
	 * @param value The value to convert to a string representation; may be null.
	 * @return The string representation; may be null.
	 * @throws HibernateException Thrown by {@link #toString(Object)}, which this calls.
	 */
	public String nullSafeToString(Object value) throws HibernateException {
		return value == null ? null : toString( value );
	}

	public abstract String toString(Object value) throws HibernateException;

	public abstract Object fromStringValue(String xml) throws HibernateException;

	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SessionImplementor session)
	throws HibernateException, SQLException {
		if ( settable[0] ) nullSafeSet(st, value, index);
	}

	public final void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
	throws HibernateException, SQLException {
		nullSafeSet(st, value, index);
	}

	public final void nullSafeSet(PreparedStatement st, Object value, int index)
	throws HibernateException, SQLException {
		try {
			if ( value == null ) {
				if ( IS_VALUE_TRACING_ENABLED ) {
					log().trace( "binding null to parameter: " + index );
				}

				st.setNull( index, sqlType() );
			}
			else {
				if ( IS_VALUE_TRACING_ENABLED ) {
					log().trace( "binding '" + toString( value ) + "' to parameter: " + index );
				}

				set( st, value, index );
			}
		}
		catch ( RuntimeException re ) {
			log().info( "could not bind value '" + nullSafeToString( value ) + "' to parameter: " + index + "; " + re.getMessage() );
			throw re;
		}
		catch ( SQLException se ) {
			log().info( "could not bind value '" + nullSafeToString( value ) + "' to parameter: " + index + "; " + se.getMessage() );
			throw se;
		}
	}

	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner)
	throws HibernateException, SQLException {
		return nullSafeGet(rs, names[0]);
	}

	public final Object nullSafeGet(ResultSet rs, String[] names)
	throws HibernateException, SQLException {
		return nullSafeGet(rs, names[0]);
	}

	public final Object nullSafeGet(ResultSet rs, String name)
	throws HibernateException, SQLException {
		try {
			Object value = get(rs, name);
			if ( value == null || rs.wasNull() ) {
				if ( IS_VALUE_TRACING_ENABLED ) {
					log().trace( "returning null as column: " + name );
				}
				return null;
			}
			else {
				if ( IS_VALUE_TRACING_ENABLED ) {
					log().trace( "returning '" + toString( value ) + "' as column: " + name );
				}
				return value;
			}
		}
		catch ( RuntimeException re ) {
			log().info( "could not read column value from result set: " + name + "; " + re.getMessage() );
			throw re;
		}
		catch ( SQLException se ) {
			log().info( "could not read column value from result set: " + name + "; " + se.getMessage() );
			throw se;
		}
	}

	public final Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
	throws HibernateException, SQLException {
		return nullSafeGet(rs, name);
	}

	public final String toXMLString(Object value, SessionFactoryImplementor pc)
	throws HibernateException {
		return toString(value);
	}

	public final Object fromXMLString(String xml, Mapping factory) throws HibernateException {
		return xml==null || xml.length()==0 ? null : fromStringValue(xml);
	}

	public final int getColumnSpan(Mapping session) {
		return 1;
	}

	public final int[] sqlTypes(Mapping session) {
		return new int[] { sqlType() };
	}

	public final boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return isEqual(x, y);
	}

	public boolean isEqual(Object x, Object y) {
		return EqualsHelper.equals(x, y);
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return value == null ? "null" : toString(value);
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return fromXMLString( xml.getText(), factory );
	}

	public void setToXMLNode(Node xml, Object value, SessionFactoryImplementor factory)
	throws HibernateException {
		xml.setText( toXMLString(value, factory) );
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value==null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
	throws HibernateException {
		return checkable[0] && isDirty(old, current, session);
	}
}
