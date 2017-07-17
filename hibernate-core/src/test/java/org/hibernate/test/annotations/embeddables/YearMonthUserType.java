package org.hibernate.test.annotations.embeddables;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.IntegerType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;

/**
 * @author András Eisenberger
 */
public class YearMonthUserType implements UserType, Serializable {
    @Override
    public int[] sqlTypes() {
        return new int[]{
                IntegerType.INSTANCE.sqlType(),
                IntegerType.INSTANCE.sqlType(),
        };
    }

    @Override
    public Class returnedClass() {
        return YearMonth.class;
    }

    @Override
    public boolean equals(final Object x, final Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        final YearMonth mtx = (YearMonth) x;
        final YearMonth mty = (YearMonth) y;
        return mtx.equals(mty);
    }

    @Override
    public int hashCode(final Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(final ResultSet rs, final String[] names, final SharedSessionContractImplementor session, final Object owner) throws HibernateException, SQLException {
        assert names.length == 2;
        final Integer year = IntegerType.INSTANCE.nullSafeGet(rs, names[0], session);
        final Integer month = IntegerType.INSTANCE.nullSafeGet(rs, names[1], session);
        return year == null || month == null ? null : YearMonth.of(year, month);
    }

    @Override
    public void nullSafeSet(final PreparedStatement st, final Object value, final int index, final SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            IntegerType.INSTANCE.set(st, null, index, session);
            IntegerType.INSTANCE.set(st, null, index + 1, session);
        } else {
            final YearMonth YearMonth = (YearMonth) value;

            IntegerType.INSTANCE.set(st, YearMonth.getYear(), index, session);
            IntegerType.INSTANCE.set(st, YearMonth.getMonthValue(), index + 1, session);
        }
    }

    @Override
    public Object deepCopy(final Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(final Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(final Serializable cached, final Object value) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
        return original;
    }
}
