//$Id: BlobType.java 7644 2005-07-25 06:53:09Z oneovthafew $
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
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.lob.BlobImpl;
import org.hibernate.lob.SerializableBlob;
import org.hibernate.util.ArrayHelper;

/**
 * <tt>blob</tt>: A type that maps an SQL BLOB to a java.sql.Blob.
 * @author Gavin King
 */
public class BlobType extends AbstractType {

	public void set(PreparedStatement st, Object value, int index, SessionImplementor session) 
	throws HibernateException, SQLException {
		
		if (value==null) {
			st.setNull(index, Types.BLOB);
		}
		else {
			
			if (value instanceof SerializableBlob) {
				value = ( (SerializableBlob) value ).getWrappedBlob();
			}
		
			final boolean useInputStream = session.getFactory().getDialect().useInputStreamToInsertBlob() && 
				(value instanceof BlobImpl);
			
			if ( useInputStream ) {
				BlobImpl blob = (BlobImpl) value;
				st.setBinaryStream( index, blob.getBinaryStream(), (int) blob.length() );
			}
			else {
				st.setBlob(index, (Blob) value);
			}
			
		}
		
	}

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
		Blob value = rs.getBlob(name);
		return rs.wasNull() ? null : new SerializableBlob(value);
	}

	public Class getReturnedClass() {
		return Blob.class;
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
		return "blob";
	}
	
	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
		throws HibernateException {
		throw new UnsupportedOperationException("Blobs are not cacheable");
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)  {
		return value;
	}
	
	public Object fromXMLNode(Node xml, Mapping factory) {
		throw new UnsupportedOperationException("todo");
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
		//Blobs are ignored by merge()
		return target;
	}
	
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return new int[] { Types.BLOB };
	}
	
	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException("todo");
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






