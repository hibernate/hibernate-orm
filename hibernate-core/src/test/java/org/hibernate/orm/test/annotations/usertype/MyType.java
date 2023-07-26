package org.hibernate.orm.test.annotations.usertype;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.EnhancedUserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class MyType implements EnhancedUserType<MyId> {
    @Override
    public String toSqlLiteral(MyId value) {
        return "'" + value.text.replace("'", "''") + "'";
    }

    @Override
    public String toString(MyId value) throws HibernateException {
        return value.text;
    }

    @Override
    public MyId fromStringValue(CharSequence sequence) throws HibernateException {
        return new MyId(sequence.toString());
    }

    @Override
    public int getSqlType() {
        return SqlTypes.VARCHAR;
    }

    @Override
    public Class<MyId> returnedClass() {
        return MyId.class;
    }

    @Override
    public boolean equals(MyId x, MyId y) {
        return x != null && y != null && x.text.equals(y.text);
    }

    @Override
    public int hashCode(MyId x) {
        return x.text.hashCode();
    }

    @Override
    public MyId nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        return new MyId(rs.getString(position));
    }

    @Override
    public void nullSafeSet(PreparedStatement st, MyId value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        st.setString(index, value.text);
    }

    @Override
    public MyId deepCopy(MyId value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(MyId value) {
        return value.text;
    }

    @Override
    public MyId assemble(Serializable cached, Object owner) {
        return new MyId((String) cached);
    }
}
