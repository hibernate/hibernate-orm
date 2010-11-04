package org.hibernate.test.annotations.cut;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.Column;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Columns;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Frode Carlsen
 */
@Columns(columns = { @Column(name = "addressLine1"), @Column(name = "cityName") })
public class AddressCompositeUserType implements CompositeUserType {

    public static final AddressCompositeUserType INSTANCE = new AddressCompositeUserType();
    private static final String[] PROPERTY_NAMES = new String[] { "addr1", "city" };

    private static final Type[] TYPES = new Type[] { StringType.INSTANCE, StringType.INSTANCE };

    public Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value, SessionImplementor session) throws HibernateException {
        return (Serializable) value;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }

    public String[] getPropertyNames() {
        return PROPERTY_NAMES;
    }

    public Type[] getPropertyTypes() {
        return TYPES;
    }

    public Object getPropertyValue(Object component, int propertyIndex) throws HibernateException {
        Address address = (Address) component;
        switch (propertyIndex) {
        case 0:
            return address.address1;
        case 1:
            return address.city;
        default:
            return null;
        }
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, Object owner)
            throws HibernateException, SQLException {
        if (resultSet == null) {
            return null;
        }
        Address address = new Address();
        String address1 = resultSet.getString(names[0]);
        String city = resultSet.getString(names[1]);
        if (address1 == null && city == null) {
            return null;
        }
        address.address1 = address1;
        address.city = city;
        return address;
    }

    public void nullSafeSet(PreparedStatement statement, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            statement.setNull(index, StringType.INSTANCE.sqlType());
            statement.setNull(index + 1, StringType.INSTANCE.sqlType());
            return;
        }
        Address address = (Address) value;
        statement.setString(index, address.address1);
        statement.setString(index + 1, address.city);
    }

    public Object replace(Object original, Object target, SessionImplementor session, Object owner)
            throws HibernateException {
        return original;
    }

    public Class<?> returnedClass() {
        return Address.class;
    }

    public void setPropertyValue(Object component, int propertyIndex, Object value) throws HibernateException {
        Address address = (Address) component;
        switch (propertyIndex) {
        case 0:
            address.address1 = (String) value;
            break;
        case 1:
            address.city = (String) value;
        default:
            break;
        }
    }
}
