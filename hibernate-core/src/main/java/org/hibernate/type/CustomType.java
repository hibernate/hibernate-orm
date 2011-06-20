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

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.dom4j.Node;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.LoggableUserType;
import org.hibernate.usertype.Sized;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.UserVersionType;

/**
 * Adapts {@link UserType} to the generic {@link Type} interface, in order
 * to isolate user code from changes in the internal Type contracts.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CustomType extends AbstractType implements IdentifierType, DiscriminatorType, VersionType, BasicType {
	private final UserType userType;
	private final String name;
	private final int[] types;
	private final Size[] dictatedSizes;
	private final Size[] defaultSizes;
	private final boolean customLogging;
	private final String[] registrationKeys;

	public CustomType(UserType userType) throws MappingException {
		this( userType, ArrayHelper.EMPTY_STRING_ARRAY );
	}

	public CustomType(UserType userType, String[] registrationKeys) throws MappingException {
		this.userType = userType;
		this.name = userType.getClass().getName();
		this.types = userType.sqlTypes();
		this.dictatedSizes = Sized.class.isInstance( userType )
				? ( (Sized) userType ).dictatedSizes()
				: new Size[ types.length ];
		this.defaultSizes = Sized.class.isInstance( userType )
				? ( (Sized) userType ).defaultSizes()
				: new Size[ types.length ];
		this.customLogging = LoggableUserType.class.isInstance( userType );
		this.registrationKeys = registrationKeys;
	}

	public UserType getUserType() {
		return userType;
	}

	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	public int[] sqlTypes(Mapping pi) {
		return types;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return dictatedSizes;
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return defaultSizes;
	}

	public int getColumnSpan(Mapping session) {
		return types.length;
	}

	public Class getReturnedClass() {
		return userType.returnedClass();
	}

	public boolean isEqual(Object x, Object y) throws HibernateException {
		return userType.equals( x, y );
	}

	public int getHashCode(Object x) {
		return userType.hashCode(x);
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return userType.nullSafeGet(rs, names, session, owner);
	}

	public Object nullSafeGet(ResultSet rs, String columnName, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet(rs, new String[] { columnName }, session, owner);
	}


	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
			throws HibernateException {
		return userType.assemble(cached, owner);
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		return userType.disassemble(value);
	}

	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache) throws HibernateException {
		return userType.replace(original, target, owner);
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			userType.nullSafeSet( st, value, index, session );
		}
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws HibernateException, SQLException {
		userType.nullSafeSet( st, value, index, session );
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public String toXMLString(Object value, SessionFactoryImplementor factory) {
		if ( value == null ) {
			return null;
		}
		if ( userType instanceof EnhancedUserType ) {
			return ( (EnhancedUserType) userType ).toXMLString( value );
		}
		else {
			return value.toString();
		}
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public Object fromXMLString(String xml, Mapping factory) {
		return ( (EnhancedUserType) userType ).fromXMLString(xml);
	}

	public String getName() {
		return name;
	}

	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		return userType.deepCopy(value);
	}

	public boolean isMutable() {
		return userType.isMutable();
	}

	public Object stringToObject(String xml) {
		return ( (EnhancedUserType) userType ).fromXMLString(xml);
	}

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return ( (EnhancedUserType) userType ).objectToSQLString(value);
	}

	public Comparator getComparator() {
		return (Comparator) userType;
	}

	public Object next(Object current, SessionImplementor session) {
		return ( (UserVersionType) userType ).next( current, session );
	}

	public Object seed(SessionImplementor session) {
		return ( (UserVersionType) userType ).seed( session );
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return fromXMLString( xml.getText(), factory );
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		node.setText( toXMLString(value, factory) );
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( value == null ) {
			return "null";
		}
		else if ( customLogging ) {
			return ( ( LoggableUserType ) userType ).toLoggableString( value, factory );
		}
		else {
			return toXMLString( value, factory );
		}
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		boolean[] result = new boolean[ getColumnSpan(mapping) ];
		if ( value != null ) {
			Arrays.fill(result, true);
		}
		return result;
	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		return checkable[0] && isDirty(old, current, session);
	}
}
