//$Id: CustomType.java 10084 2006-07-05 15:03:52Z steve.ebersole@jboss.com $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.UserVersionType;
import org.hibernate.usertype.LoggableUserType;

/**
 * Adapts {@link UserType} to the generic {@link Type} interface, in order
 * to isolate user code from changes in the internal Type contracts.
 *
 * @see org.hibernate.usertype.UserType
 * @author Gavin King
 */
public class CustomType extends AbstractType implements IdentifierType, DiscriminatorType, VersionType {

	private final UserType userType;
	private final String name;
	private final int[] types;
	private final boolean customLogging;

	public CustomType(Class userTypeClass, Properties parameters) throws MappingException {

		if ( !UserType.class.isAssignableFrom( userTypeClass ) ) {
			throw new MappingException(
					"Custom type does not implement UserType: " +
					userTypeClass.getName()
				);
		}

		name = userTypeClass.getName();

		try {
			userType = ( UserType ) userTypeClass.newInstance();
		}
		catch ( InstantiationException ie ) {
			throw new MappingException(
					"Cannot instantiate custom type: " +
					userTypeClass.getName()
				);
		}
		catch ( IllegalAccessException iae ) {
			throw new MappingException(
					"IllegalAccessException trying to instantiate custom type: " +
					userTypeClass.getName()
				);
		}

        TypeFactory.injectParameters( userType, parameters );
		types = userType.sqlTypes();

		customLogging = LoggableUserType.class.isAssignableFrom( userTypeClass );
	}

	public int[] sqlTypes(Mapping pi) {
		return types;
	}

	public int getColumnSpan(Mapping session) {
		return types.length;
	}

	public Class getReturnedClass() {
		return userType.returnedClass();
	}

	public boolean isEqual(Object x, Object y) throws HibernateException {
		return userType.equals(x, y);
	}

	public boolean isEqual(Object x, Object y, EntityMode entityMode)
	throws HibernateException {
		return isEqual(x, y);
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		return userType.hashCode(x);
	}

	public Object nullSafeGet(
		ResultSet rs,
		String[] names,
		SessionImplementor session,
		Object owner
	) throws HibernateException, SQLException {

		return userType.nullSafeGet(rs, names, owner);
	}

	public Object nullSafeGet(
		ResultSet rs,
		String columnName,
		SessionImplementor session,
		Object owner
	) throws HibernateException, SQLException {
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
			Map copyCache)
	throws HibernateException {
		return userType.replace(original, target, owner);
	}

	public void nullSafeSet(
		PreparedStatement st,
		Object value,
		int index,
		boolean[] settable,
		SessionImplementor session
	) throws HibernateException, SQLException {

		if ( settable[0] ) userType.nullSafeSet(st, value, index);
	}

	public void nullSafeSet(
		PreparedStatement st,
		Object value,
		int index,
		SessionImplementor session
	) throws HibernateException, SQLException {

		userType.nullSafeSet(st, value, index);
	}

	public String toXMLString(Object value, SessionFactoryImplementor factory) {
		if (value==null) return null;
		if (userType instanceof EnhancedUserType) {
			return ( (EnhancedUserType) userType ).toXMLString(value);
		}
		else {
			return value.toString();
		}
	}

	public Object fromXMLString(String xml, Mapping factory) {
		return ( (EnhancedUserType) userType ).fromXMLString(xml);
	}

	public String getName() {
		return name;
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)
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
		if (value!=null) Arrays.fill(result, true);
		return result;
	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) throws HibernateException {
		return checkable[0] && isDirty(old, current, session);
	}

}
