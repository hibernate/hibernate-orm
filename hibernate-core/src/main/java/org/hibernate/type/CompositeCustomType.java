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
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.spi.relational.Size;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.LoggableUserType;

import org.dom4j.Element;
import org.dom4j.Node;

/**
 * Adapts {@link CompositeUserType} to the {@link Type} interface
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CompositeCustomType extends AbstractType implements CompositeType, BasicType {
	private final CompositeUserType userType;
	private final String[] registrationKeys;
	private final String name;
	private final boolean customLogging;

	public CompositeCustomType(CompositeUserType userType) {
		this( userType, ArrayHelper.EMPTY_STRING_ARRAY );
	}

	public CompositeCustomType(CompositeUserType userType, String[] registrationKeys) {
		this.userType = userType;
		this.name = userType.getClass().getName();
		this.customLogging = LoggableUserType.class.isInstance( userType );
		this.registrationKeys = registrationKeys;
	}

	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	public CompositeUserType getUserType() {
		return userType;
	}

	public boolean isMethodOf(Method method) {
		return false;
	}

	public Type[] getSubtypes() {
		return userType.getPropertyTypes();
	}

	public String[] getPropertyNames() {
		return userType.getPropertyNames();
	}

	public Object[] getPropertyValues(Object component, SessionImplementor session) throws HibernateException {
		return getPropertyValues( component, EntityMode.POJO );
	}

	public Object[] getPropertyValues(Object component, EntityMode entityMode) throws HibernateException {

		int len = getSubtypes().length;
		Object[] result = new Object[len];
		for ( int i=0; i<len; i++ ) {
			result[i] = getPropertyValue(component, i);
		}
		return result;
	}

	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode)
		throws HibernateException {

		for (int i=0; i<values.length; i++) {
			userType.setPropertyValue( component, i, values[i] );
		}
	}

	public Object getPropertyValue(Object component, int i, SessionImplementor session)
		throws HibernateException {
		return getPropertyValue(component, i);
	}

	public Object getPropertyValue(Object component, int i) throws HibernateException {
		return userType.getPropertyValue( component, i );
	}

	public CascadeStyle getCascadeStyle(int i) {
		return CascadeStyles.NONE;
	}

	public FetchMode getFetchMode(int i) {
		return FetchMode.DEFAULT;
	}

	public boolean isComponentType() {
		return true;
	}

	public Object deepCopy(Object value, SessionFactoryImplementor factory)
	throws HibernateException {
		return userType.deepCopy( value );
	}

	public Object assemble(
		Serializable cached,
		SessionImplementor session,
		Object owner)
		throws HibernateException {

		return userType.assemble( cached, session, owner );
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {
		return userType.disassemble(value, session);
	}

	public Object replace(
			Object original, 
			Object target,
			SessionImplementor session, 
			Object owner, 
			Map copyCache)
	throws HibernateException {
		return userType.replace(original, target, session, owner);
	}
	
	public boolean isEqual(Object x, Object y)
	throws HibernateException {
		return userType.equals(x, y);
	}

	public int getHashCode(Object x) {
		return userType.hashCode(x);
	}
	
	public int getColumnSpan(Mapping mapping) throws MappingException {
		Type[] types = userType.getPropertyTypes();
		int n=0;
		for ( Type type : types ) {
			n += type.getColumnSpan( mapping );
		}
		return n;
	}

	public String getName() {
		return name;
	}

	public Class getReturnedClass() {
		return userType.returnedClass();
	}

	public boolean isMutable() {
		return userType.isMutable();
	}

	public Object nullSafeGet(
		ResultSet rs,
		String columnName,
		SessionImplementor session,
		Object owner)
		throws HibernateException, SQLException {

		return userType.nullSafeGet( rs, new String[] {columnName}, session, owner );
	}

	public Object nullSafeGet(
		ResultSet rs,
		String[] names,
		SessionImplementor session,
		Object owner)
		throws HibernateException, SQLException {

		return userType.nullSafeGet(rs, names, session, owner);
	}

	public void nullSafeSet(
		PreparedStatement st,
		Object value,
		int index,
		SessionImplementor session)
		throws HibernateException, SQLException {

		userType.nullSafeSet(st, value, index, session);

	}

	public void nullSafeSet(
		PreparedStatement st,
		Object value,
		int index,
		boolean[] settable, 
		SessionImplementor session)
		throws HibernateException, SQLException {

		userType.nullSafeSet(st, value, index, session);
	}

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		int[] result = new int[ getColumnSpan(mapping) ];
		int n=0;
		for ( Type type : userType.getPropertyTypes() ) {
			for ( int sqlType : type.sqlTypes( mapping ) ) {
				result[n++] = sqlType;
			}
		}
		return result;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		//Not called at runtime so doesn't matter if its slow :)
		final Size[] sizes = new Size[ getColumnSpan( mapping ) ];
		int soFar = 0;
		for ( Type propertyType : userType.getPropertyTypes() ) {
			final Size[] propertySizes = propertyType.dictatedSizes( mapping );
			System.arraycopy( propertySizes, 0, sizes, soFar, propertySizes.length );
			soFar += propertySizes.length;
		}
		return sizes;
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		//Not called at runtime so doesn't matter if its slow :)
		final Size[] sizes = new Size[ getColumnSpan( mapping ) ];
		int soFar = 0;
		for ( Type propertyType : userType.getPropertyTypes() ) {
			final Size[] propertySizes = propertyType.defaultSizes( mapping );
			System.arraycopy( propertySizes, 0, sizes, soFar, propertySizes.length );
			soFar += propertySizes.length;
		}
		return sizes;
	}
	
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		if ( value == null ) {
			return "null";
		}
		else if ( customLogging ) {
			return ( (LoggableUserType) userType ).toLoggableString( value, factory );
		}
		else {
			return value.toString();
		}
	}

	public boolean[] getPropertyNullability() {
		return null;
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return xml;
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory)
	throws HibernateException {
		replaceNode( node, (Element) value );
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		boolean[] result = new boolean[ getColumnSpan(mapping) ];
		if (value==null) return result;
		Object[] values = getPropertyValues(value, EntityMode.POJO); //TODO!!!!!!!
		int loc = 0;
		Type[] propertyTypes = getSubtypes();
		for ( int i=0; i<propertyTypes.length; i++ ) {
			boolean[] propertyNullness = propertyTypes[i].toColumnNullness( values[i], mapping );
			System.arraycopy(propertyNullness, 0, result, loc, propertyNullness.length);
			loc += propertyNullness.length;
		}
		return result;
	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) throws HibernateException {
		return isDirty(old, current, session);
	}
	
	public boolean isEmbedded() {
		return false;
	}
}
