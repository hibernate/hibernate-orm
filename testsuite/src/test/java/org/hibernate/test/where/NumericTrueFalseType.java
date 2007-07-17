package org.hibernate.test.where;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.hibernate.type.BooleanType;
import org.hibernate.dialect.Dialect;

/**
 * Maps int db values to boolean java values.  Zero is considered false; any
 * non-zero value is considered true.
 *
 * @author Steve Ebersole
 */
public class NumericTrueFalseType extends BooleanType {

	public Object get(ResultSet rs, String name) throws SQLException {
		int value = rs.getInt( name );
		if ( rs.wasNull() ) {
			return getDefaultValue();
		}
		else if ( value == 0 ) {
			return Boolean.FALSE;
		}
		else {
			return Boolean.TRUE;
		}
	}

	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.INTEGER );
		}
		else {
			boolean bool = ( ( Boolean ) value ).booleanValue();
			st.setInt( index, bool ? 1 : 0 );
		}
	}

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return ( ( Boolean ) value ).booleanValue() ? "1" : "0";
	}

	public int sqlType() {
		return Types.INTEGER;
	}

	public String getName() {
		return "numeric_boolean";
	}
}
