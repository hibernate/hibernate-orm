//$Id: FloatType.java 7825 2005-08-10 20:23:55Z oneovthafew $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.Dialect;

/**
 * <tt>float</tt>: A type that maps an SQL FLOAT to a Java Float.
 * @author Gavin King
 */
public class FloatType extends PrimitiveType {

	public Serializable getDefaultValue() {
		return new Float(0.0);
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return new Float( rs.getFloat(name) );
	}

	public Class getPrimitiveClass() {
		return float.class;
	}

	public Class getReturnedClass() {
		return Float.class;
	}

	public void set(PreparedStatement st, Object value, int index)
	throws SQLException {

		st.setFloat( index, ( (Float) value ).floatValue() );
	}

	public int sqlType() {
		return Types.FLOAT;
	}

	public String getName() { return "float"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return value.toString();
	}

	public Object fromStringValue(String xml) {
		return new Float(xml);
	}

}





