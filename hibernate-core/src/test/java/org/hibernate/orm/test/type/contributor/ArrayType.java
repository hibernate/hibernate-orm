package org.hibernate.orm.test.type.contributor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcTypeDescriptor;
import org.hibernate.usertype.UserType;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType implements UserType<Array>, AllowableParameterType<Array> {
    public static final ArrayType INSTANCE = new ArrayType();

    private final BasicJavaTypeDescriptor<Array> javaType = ArrayTypeDescriptor.INSTANCE;
    private final JdbcTypeDescriptor jdbcType = VarcharJdbcTypeDescriptor.INSTANCE;

    @Override
    public BasicJavaTypeDescriptor<Array> getExpressableJavaTypeDescriptor() {
        return javaType;
    }

    @Override
    public PersistenceType getPersistenceType() {
        return PersistenceType.BASIC;
    }

    @Override
    public Class<Array> getJavaType() {
        return Array.class;
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
