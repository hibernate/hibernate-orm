package org.hibernate.test.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;

/**
 * @author Christian Beikov
 */
public class StringWrapperUserType implements UserType {

	public static final StringWrapperUserType INSTANCE = new StringWrapperUserType();

    private static final Logger log = Logger.getLogger( StringWrapperUserType.class );

    @Override
    public int[] sqlTypes() {
        return new int[] {StringType.INSTANCE.sqlType()};
    }

    @Override
    public Class returnedClass() {
        return StringWrapper.class;
    }

    @Override
    public boolean equals(Object x, Object y)
			throws HibernateException {
        return Objects.equals( x, y );
    }

    @Override
    public int hashCode(Object x)
			throws HibernateException {
        return Objects.hashCode( x );
    }

    @Override
    public Object nullSafeGet(
            ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String columnName = names[0];
        String columnValue = (String) rs.getObject( columnName );
        log.debugv("Result set column {0} value is {1}", columnName, columnValue);
        return columnValue == null ? null :
				fromString( columnValue );
    }

    @Override
    public void nullSafeSet(
            PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if ( value == null ) {
            log.debugv("Binding null to parameter {0} ",index);
            st.setNull( index, Types.VARCHAR );
        }
        else {
            String stringValue = toString( (StringWrapper) value );
            log.debugv("Binding {0} to parameter {1} ", stringValue, index);
            st.setString( index, stringValue );
        }
    }
    public String toString(StringWrapper value) {
        return value.getValue();
    }

    public StringWrapper fromString(String string) {
        if ( string == null || string.isEmpty() ) {
            return null;
        }
        return new StringWrapper( string );
    }

    @Override
    public Object deepCopy(Object value)
			throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value)
			throws HibernateException {
        return (StringWrapper) deepCopy( value );
    }

    @Override
    public Object assemble(Serializable cached, Object owner)
			throws HibernateException {
        return deepCopy( cached );
    }

    @Override
    public Object replace(Object original, Object target, Object owner)
			throws HibernateException {
        return deepCopy( original );
    }
}
