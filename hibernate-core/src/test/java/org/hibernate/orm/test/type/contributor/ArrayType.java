package org.hibernate.orm.test.type.contributor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.UserType;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType implements UserType<Array>, BindableType<Array> {
    public static final ArrayType INSTANCE = new ArrayType();

    private final BasicJavaType<Array> javaType = ArrayTypeDescriptor.INSTANCE;
    private final JdbcType jdbcType = VarcharJdbcType.INSTANCE;

    @Override
    public Class<Array> getBindableJavaType() {
        // really a UserType should not implement BindableType
        return Array.class;
    }

    @Override
    public SqmExpressable<Array> resolveExpressable(SessionFactoryImplementor sessionFactory) {
        // really a UserType should not implement BindableType
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] sqlTypes() {
        return new int[] { jdbcType.getJdbcTypeCode() };
    }

    @Override
    public Class<Array> returnedClass() {
        return Array.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return javaType.areEqual( (Array) x, (Array) y );
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return javaType.extractHashCode( (Array) x );
    }

    @Override
    public Array nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        return jdbcType.getExtractor( javaType ).extract( rs, position, session );
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Array value, int index, SharedSessionContractImplementor session) throws SQLException {
        jdbcType.getBinder( javaType ).bind( st, value, index, session );

    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return ArrayMutabilityPlan.INSTANCE.deepCopy( (Array) value );
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return ArrayMutabilityPlan.INSTANCE.disassemble( (Array) value, null );
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return ArrayMutabilityPlan.INSTANCE.assemble( cached, null );
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return null;
    }
}
