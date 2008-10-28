//$Id$
package org.hibernate.type;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.lob.BlobImpl;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.SerializationHelper;

/**
 * @author Emmanuel Bernard
 */
public class SerializableToBlobType extends AbstractLobType implements ParameterizedType {
	/**
	 * class name of the serialisable class
	 */
	public static final String CLASS_NAME = "classname";
	private Class serializableClass;
	private SerializableType type;

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return new int[]{Types.BLOB};
	}

	public Class getReturnedClass() {
		return serializableClass;
	}

	@Override
	public boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory) {
		return type.isEqual( x, y );
	}


	@Override
	public int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor session) {
		return type.getHashCode( x, null );
	}

	public Object get(ResultSet rs, String name) throws SQLException {
		Blob blob = rs.getBlob( name );
		if ( rs.wasNull() ) return null;
		int length = (int) blob.length();
		byte[] primaryResult = blob.getBytes( 1, length );
		return fromBytes( primaryResult );
	}

	private static byte[] toBytes(Object object) throws SerializationException {
		return SerializationHelper.serialize( (Serializable) object );
	}

	private static Object fromBytes(byte[] bytes) throws SerializationException {
		return SerializationHelper.deserialize( bytes );
	}

	public void set(PreparedStatement st, Object value, int index, SessionImplementor session) throws SQLException {
		if ( value != null ) {
			byte[] toSet;
			toSet = toBytes( value );
			if ( session.getFactory().getDialect().useInputStreamToInsertBlob() ) {
				st.setBinaryStream( index, new ByteArrayInputStream( toSet ), toSet.length );
			}
			else {
				st.setBlob( index, new BlobImpl( toSet ) );
			}
		}
		else {
			st.setNull( index, sqlTypes( null )[0] );
		}
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) throws HibernateException {
		type.setToXMLNode( node, value, factory );
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return type.toLoggableString( value, factory );
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return type.fromXMLNode( xml, factory );
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)
			throws HibernateException {
		return type.deepCopy( value, null, null );
	}

	public boolean isMutable() {
		return type.isMutable();
	}

	public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		return type.replace( original, target, session, owner, copyCache );
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return type.toColumnNullness( value, mapping );
	}

	public void setParameterValues(Properties parameters) {
		if ( parameters != null ) {
			String className = parameters.getProperty( CLASS_NAME );
			if ( className == null ) {
				throw new MappingException(
						"No class name defined for type: " + SerializableToBlobType.class.getName()
				);
			}
			try {
				serializableClass = ReflectHelper.classForName( className );
			}
			catch (ClassNotFoundException e) {
				throw new MappingException( "Unable to load class from " + CLASS_NAME + " parameter", e );
			}
		}
		type = new SerializableType( serializableClass );
	}
}
