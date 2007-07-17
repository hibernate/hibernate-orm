//$Id: BigDecimalType.java 5744 2005-02-16 12:50:19Z oneovthafew $
package org.hibernate.type;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;

/**
 * <tt>big_decimal</tt>: A type that maps an SQL NUMERIC to a
 * <tt>java.math.BigDecimal</tt>
 * @see java.math.BigDecimal
 * @author Gavin King
 */
public class BigDecimalType extends ImmutableType {

	public Object get(ResultSet rs, String name)
	throws HibernateException, SQLException {
		return rs.getBigDecimal(name);
	}

	public void set(PreparedStatement st, Object value, int index)
	throws HibernateException, SQLException {
		st.setBigDecimal(index, (BigDecimal) value);
	}

	public int sqlType() {
		return Types.NUMERIC;
	}

	public String toString(Object value) throws HibernateException {
		return value.toString();
	}

	public Class getReturnedClass() {
		return BigDecimal.class;
	}

	public boolean isEqual(Object x, Object y) {
		return x==y || ( x!=null && y!=null && ( (BigDecimal) x ).compareTo( (BigDecimal) y )==0 );
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		return ( (BigDecimal) x ).intValue();
	}

	public String getName() {
		return "big_decimal";
	}

	public Object fromStringValue(String xml) {
		return new BigDecimal(xml);
	}


}






