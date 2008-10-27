package org.jboss.envers.test.entities.customtype;

import org.hibernate.HibernateException;
import org.hibernate.Hibernate;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.io.Serializable;

/**
 * @author Andrew DePue
 * @author Adam Warski (adam at warski dot org)
 */
public class CompositeTestUserType implements CompositeUserType {
    public String[] getPropertyNames() {
        return new String[] { "prop1", "prop2" };
    }

    public Type[] getPropertyTypes() {
        return new Type[] { Hibernate.STRING, Hibernate.INTEGER };
    }

    public Object getPropertyValue(final Object component, final int property) throws HibernateException {
        Component comp = (Component) component;
        if (property == 0) {
            return comp.getProp1();
        } else {
            return comp.getProp2();
        }
    }

    public void setPropertyValue(final Object component, final int property, final Object value) throws HibernateException {
        Component comp = (Component) component;
        if (property == 0) {
            comp.setProp1((String) value);
        } else {
            comp.setProp2((Integer) value);
        }
    }

    public Class returnedClass() {
        return Component.class;
    }

    public boolean equals(final Object x, final Object y) throws HibernateException {
        //noinspection ObjectEquality
        if (x == y) {
            return true;
        }

        if (x == null || y == null) {
            return false;
        }

        return x.equals(y);
    }

    public int hashCode(final Object x) throws HibernateException {
        return x.hashCode();
    }

    public Object nullSafeGet(final ResultSet rs, final String[] names,
                              final SessionImplementor session,
                              final Object owner) throws HibernateException, SQLException {
        if (rs.wasNull()) {
            return null;
        }
        final String prop1 = rs.getString(names[0]);
        if (prop1 == null) {
            return null;
        }
        final int prop2 = rs.getInt(names[1]);
        
        return new Component(prop1, prop2);
    }

    public void nullSafeSet(final PreparedStatement st, final Object value,
                            final int index, final SessionImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            st.setNull(index, Hibernate.STRING.sqlType());
            st.setNull(index + 1, Hibernate.INTEGER.sqlType());
        } else {
            final Component comp = (Component) value;
            st.setString(index, comp.getProp1());
            st.setInt(index + 1, comp.getProp2());
        }
    }

    public Object deepCopy(final Object value) throws HibernateException {
        Component comp = (Component) value;
        return new Component(comp.getProp1(), comp.getProp2());
    }

    public boolean isMutable() {
        return true;
    }

    public Serializable disassemble(final Object value, final SessionImplementor session) throws HibernateException {
        return (Serializable) value;
    }

    public Object assemble(final Serializable cached, final SessionImplementor session,
                           final Object owner) throws HibernateException {
        return cached;
    }

    public Object replace(Object original, Object target,
                          SessionImplementor session, Object owner) throws HibernateException {
        return original;
    }
}