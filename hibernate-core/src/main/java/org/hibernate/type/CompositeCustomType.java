/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.LoggableUserType;

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

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class getReturnedClass() {
		return userType.returnedClass();
	}

	@Override
	public boolean isMutable() {
		return userType.isMutable();
	}

	@Override
	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	public CompositeUserType getUserType() {
		return userType;
	}

	@Override
	public boolean isMethodOf(Method method) {
		return false;
	}

	@Override
	public Type[] getSubtypes() {
		return userType.getPropertyTypes();
	}

	public String[] getPropertyNames() {
		return userType.getPropertyNames();
	}

	@Override
	public int getPropertyIndex(String name) {
		String[] names = getPropertyNames();
		for ( int i = 0, max = names.length; i < max; i++ ) {
			if ( names[i].equals( name ) ) {
				return i;
			}
		}
		throw new PropertyNotFoundException(
				"Unable to locate property named " + name + " on " + getReturnedClass().getName()
		);
	}

	@Override
	public Object[] getPropertyValues(Object component, SharedSessionContractImplementor session) throws HibernateException {
		return getPropertyValues( component, EntityMode.POJO );
	}

	@Override
	public Object[] getPropertyValues(Object component, EntityMode entityMode) throws HibernateException {
		int len = getSubtypes().length;
		Object[] result = new Object[len];
		for ( int i = 0; i < len; i++ ) {
			result[i] = getPropertyValue( component, i );
		}
		return result;
	}

	@Override
	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode) throws HibernateException {
		for ( int i = 0; i < values.length; i++ ) {
			userType.setPropertyValue( component, i, values[i] );
		}
	}

	@Override
	public Object getPropertyValue(Object component, int i, SharedSessionContractImplementor session) throws HibernateException {
		return getPropertyValue( component, i );
	}

	public Object getPropertyValue(Object component, int i) throws HibernateException {
		return userType.getPropertyValue( component, i );
	}

	@Override
	public CascadeStyle getCascadeStyle(int i) {
		return CascadeStyles.NONE;
	}

	@Override
	public FetchMode getFetchMode(int i) {
		return FetchMode.DEFAULT;
	}

	@Override
	public boolean isComponentType() {
		return true;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		return userType.deepCopy( value );
	}

	@Override
	public Object assemble(
			Serializable cached,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException {
		return userType.assemble( cached, session, owner );
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return userType.disassemble( value, session );
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache)
			throws HibernateException {
		return userType.replace( original, target, session, owner );
	}

	@Override
	public boolean isEqual(Object x, Object y)
			throws HibernateException {
		return userType.equals( x, y );
	}

	@Override
	public int getHashCode(Object x) {
		return userType.hashCode( x );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		Type[] types = userType.getPropertyTypes();
		int n = 0;
		for ( Type type : types ) {
			n += type.getColumnSpan( mapping );
		}
		return n;
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String columnName,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException, SQLException {
		return userType.nullSafeGet( rs, new String[] {columnName}, session, owner );
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException, SQLException {
		return userType.nullSafeGet( rs, names, session, owner );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		userType.nullSafeSet( st, value, index, session );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		userType.nullSafeSet( st, value, index, session );
	}

	@Override
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		int[] result = new int[getColumnSpan( mapping )];
		int n = 0;
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
		final Size[] sizes = new Size[getColumnSpan( mapping )];
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
		final Size[] sizes = new Size[getColumnSpan( mapping )];
		int soFar = 0;
		for ( Type propertyType : userType.getPropertyTypes() ) {
			final Size[] propertySizes = propertyType.defaultSizes( mapping );
			System.arraycopy( propertySizes, 0, sizes, soFar, propertySizes.length );
			soFar += propertySizes.length;
		}
		return sizes;
	}

	@Override
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

	@Override
	public boolean[] getPropertyNullability() {
		return null;
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		boolean[] result = new boolean[getColumnSpan( mapping )];
		if ( value == null ) {
			return result;
		}
		Object[] values = getPropertyValues( value, EntityMode.POJO ); //TODO!!!!!!!
		int loc = 0;
		Type[] propertyTypes = getSubtypes();
		for ( int i = 0; i < propertyTypes.length; i++ ) {
			boolean[] propertyNullness = propertyTypes[i].toColumnNullness( values[i], mapping );
			System.arraycopy( propertyNullness, 0, result, loc, propertyNullness.length );
			loc += propertyNullness.length;
		}
		return result;
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return isDirty( old, current, session );
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}

	@Override
	public boolean hasNotNullProperty() {
		// We just don't know.  So assume nullable
		return false;
	}
}
