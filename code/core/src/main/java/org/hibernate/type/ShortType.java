//$Id: ShortType.java 7825 2005-08-10 20:23:55Z oneovthafew $
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
 * <tt>short</tt>: A type that maps an SQL SMALLINT to a Java Short.
 * @author Gavin King
 */
public class ShortType extends PrimitiveType  implements DiscriminatorType, VersionType {

	private static final Short ZERO = new Short( (short) 0 );

	public Serializable getDefaultValue() {
		return ZERO;
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return new Short( rs.getShort(name) );
	}

	public Class getPrimitiveClass() {
		return short.class;
	}

	public Class getReturnedClass() {
		return Short.class;
	}

	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		st.setShort( index, ( (Short) value ).shortValue() );
	}

	public int sqlType() {
		return Types.SMALLINT;
	}

	public String getName() { return "short"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return value.toString();
	}

	public Object stringToObject(String xml) throws Exception {
		return new Short(xml);
	}

	public Object next(Object current, SessionImplementor session) {
		return new Short( (short) ( ( (Short) current ).shortValue() + 1 ) );
	}

	public Object seed(SessionImplementor session) {
		return ZERO;
	}

	public Comparator getComparator() {
		return ComparableComparator.INSTANCE;
	}
	
	public Object fromStringValue(String xml) {
		return new Short(xml);
	}

}





