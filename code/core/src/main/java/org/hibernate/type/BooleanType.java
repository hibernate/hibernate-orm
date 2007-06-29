//$Id: BooleanType.java 7825 2005-08-10 20:23:55Z oneovthafew $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.Dialect;

/**
 * <tt>boolean</tt>: A type that maps an SQL BIT to a Java Boolean.
 * @author Gavin King
 */
public class BooleanType extends PrimitiveType implements DiscriminatorType {

	public Serializable getDefaultValue() {
		return Boolean.FALSE;
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return rs.getBoolean(name) ? Boolean.TRUE : Boolean.FALSE;
	}

	public Class getPrimitiveClass() {
		return boolean.class;
	}

	public Class getReturnedClass() {
		return Boolean.class;
	}

	public void set(PreparedStatement st, Object value, int index)
	throws SQLException {
		st.setBoolean( index, ( (Boolean) value ).booleanValue() );
	}

	public int sqlType() {
		return Types.BIT;
	}

	public String getName() { return "boolean"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return dialect.toBooleanValueString( ( (Boolean) value ).booleanValue() );
	}

	public Object stringToObject(String xml) throws Exception {
		return fromStringValue(xml);
	}

	public Object fromStringValue(String xml) {
		return Boolean.valueOf(xml);
	}

}





