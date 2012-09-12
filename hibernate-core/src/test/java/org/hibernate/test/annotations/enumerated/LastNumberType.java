package org.hibernate.test.annotations.enumerated;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Janario Oliveira
 */
public class LastNumberType extends org.hibernate.type.EnumType {

	@Override
	public int[] sqlTypes() {
		return new int[] { Types.VARCHAR };
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		String persistValue = (String) rs.getObject( names[0] );
		if ( rs.wasNull() ) {
			return null;
		}
		return Enum.valueOf( returnedClass(), "NUMBER_" + persistValue );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, sqlTypes()[0] );
		}
		else {

			String enumString = ( (Enum<?>) value ).name();
			// Using setString here, rather than setObject.  A few JDBC drivers
			// (Oracle, DB2, and SQLServer) were having trouble converting
			// the char to VARCHAR.
			st.setString( index, enumString.substring( enumString.length() - 1 ) );
		}
	}
}
