/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated.custom_types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Janario Oliveira
 */
public class FirstLetterType extends org.hibernate.type.EnumType {

	@Override
	public int[] sqlTypes() {
		return new int[] { Types.VARCHAR };
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		String persistValue = (String) rs.getObject( names[0] );
		if ( rs.wasNull() ) {
			return null;
		}
		return Enum.valueOf( returnedClass(), persistValue + "_LETTER" );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, sqlTypes()[0] );
		}
		else {
			String enumString = ( (Enum<?>) value ).name();
			// Using setString here, rather than setObject.  A few JDBC drivers
			// (Oracle, DB2, and SQLServer) were having trouble converting
			// the char to VARCHAR.
			st.setString( index, enumString.substring( 0, 1 ) );
		}
	}
}
