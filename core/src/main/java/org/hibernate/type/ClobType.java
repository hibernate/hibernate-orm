//$Id: ClobType.java 7644 2005-07-25 06:53:09Z oneovthafew $
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
import org.hibernate.lob.ClobImpl;
import org.hibernate.lob.SerializableClob;
import org.hibernate.util.ArrayHelper;

/**
 * <tt>clob</tt>: A type that maps an SQL CLOB to a java.sql.Clob.
 * @author Gavin King
 */
public class ClobType extends AbstractType {

	public void set(PreparedStatement st, Object value, int index, SessionImplementor session) 
	throws HibernateException, SQLException {
		
		if (value==null) {
			st.setNull(index, Types.CLOB);
		}
		else {
		
			if (value instanceof SerializableClob) {
				value = ( (SerializableClob) value ).getWrappedClob();
			}
		
			final boolean useReader = session.getFactory().getDialect().useInputStreamToInsertBlob() && 
				(value instanceof ClobImpl);
			
			if ( useReader ) {
				ClobImpl clob = (ClobImpl) value;
				st.setCharacterStream( index, clob.getCharacterStream(), (int) clob.length() );
			}
			else {
				st.setClob(index, (Clob) value);
			}
			
		}
		
	}

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
		Clob value = rs.getClob(name);
		return rs.wasNull() ? null : new SerializableClob(value);
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
	
	public Object nullSafeGet(ResultSet rs, String name,
			SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return get(rs, name);
	}
	
	public Object nullSafeGet(ResultSet rs, String[] names,
			SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return get( rs, names[0] );
	}
	
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			boolean[] settable, SessionImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) set(st, value, index, session);
	}
	
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			SessionImplementor session) throws HibernateException, SQLException {
		set(st, value, index, session);
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






