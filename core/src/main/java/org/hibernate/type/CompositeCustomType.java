//$Id: CompositeCustomType.java 7670 2005-07-29 05:36:14Z oneovthafew $
package org.hibernate.type;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Element;
import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.usertype.CompositeUserType;

/**
 * Adapts <tt>CompositeUserType</tt> to <tt>Type</tt> interface
 * @author Gavin King
 */
public class CompositeCustomType extends AbstractType
	implements AbstractComponentType {

	private final CompositeUserType userType;
	private final String name;

	public CompositeCustomType(Class userTypeClass, Properties parameters) 
	throws MappingException {
		name = userTypeClass.getName();

		if ( !CompositeUserType.class.isAssignableFrom(userTypeClass) ) {
			throw new MappingException( 
					"Custom type does not implement CompositeUserType: " + 
					userTypeClass.getName() 
				);
		}
		
		try {
			userType = (CompositeUserType) userTypeClass.newInstance();
		}
		catch (InstantiationException ie) {
			throw new MappingException( 
					"Cannot instantiate custom type: " + 
					userTypeClass.getName() 
				);
		}
		catch (IllegalAccessException iae) {
			throw new MappingException( 
					"IllegalAccessException trying to instantiate custom type: " + 
					userTypeClass.getName() 
				);
		}
		TypeFactory.injectParameters(userType, parameters);
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

	public Object[] getPropertyValues(Object component, SessionImplementor session)
		throws HibernateException {
		return getPropertyValues( component, session.getEntityMode() );
	}

	public Object[] getPropertyValues(Object component, EntityMode entityMode)
		throws HibernateException {

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

	public Object getPropertyValue(Object component, int i)
		throws HibernateException {
		return userType.getPropertyValue(component, i);
	}

	public CascadeStyle getCascadeStyle(int i) {
		return CascadeStyle.NONE;
	}

	public FetchMode getFetchMode(int i) {
		return FetchMode.DEFAULT;
	}

	public boolean isComponentType() {
		return true;
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) 
	throws HibernateException {
		return userType.deepCopy(value);
	}

	public Object assemble(
		Serializable cached,
		SessionImplementor session,
		Object owner)
		throws HibernateException {

		return userType.assemble(cached, session, owner);
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
	
	public boolean isEqual(Object x, Object y, EntityMode entityMode) 
	throws HibernateException {
		return userType.equals(x, y);
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		return userType.hashCode(x);
	}
	
	public int getColumnSpan(Mapping mapping) throws MappingException {
		Type[] types = userType.getPropertyTypes();
		int n=0;
		for (int i=0; i<types.length; i++) {
			n+=types[i].getColumnSpan(mapping);
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

		return userType.nullSafeGet(rs, new String[] {columnName}, session, owner);
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
		Type[] types = userType.getPropertyTypes();
		int[] result = new int[ getColumnSpan(mapping) ];
		int n=0;
		for (int i=0; i<types.length; i++) {
			int[] sqlTypes = types[i].sqlTypes(mapping);
			for ( int k=0; k<sqlTypes.length; k++ ) result[n++] = sqlTypes[k];
		}
		return result;
	}
	
	public String toLoggableString(Object value, SessionFactoryImplementor factory)
		throws HibernateException {

		return value==null ? "null" : value.toString();
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
