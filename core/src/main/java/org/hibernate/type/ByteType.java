//$Id: ByteType.java 7825 2005-08-10 20:23:55Z oneovthafew $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;

import org.hibernate.util.ComparableComparator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;

/**
 * <tt>byte</tt>: A type that maps an SQL TINYINT to a Java Byte.
 * @author Gavin King
 */
public class ByteType extends PrimitiveType implements DiscriminatorType, VersionType {

	private static final Byte ZERO = new Byte( (byte) 0 );

	public Serializable getDefaultValue() {
		return ZERO;
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return new Byte( rs.getByte(name) );
	}

	public Class getPrimitiveClass() {
		return byte.class;
	}

	public Class getReturnedClass() {
		return Byte.class;
	}

	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		st.setByte( index, ( (Byte) value ).byteValue() );
	}

	public int sqlType() {
		return Types.TINYINT;
	}

	public String getName() { return "byte"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return value.toString();
	}

	public Object stringToObject(String xml) throws Exception {
		return new Byte(xml);
	}

	public Object fromStringValue(String xml) {
		return new Byte(xml);
	}

	public Object next(Object current, SessionImplementor session) {
		return new Byte( (byte) ( ( (Byte) current ).byteValue() + 1 ) );
	}

	public Object seed(SessionImplementor session) {
		return ZERO;
	}

	public Comparator getComparator() {
		return ComparableComparator.INSTANCE;
	}
	
}
