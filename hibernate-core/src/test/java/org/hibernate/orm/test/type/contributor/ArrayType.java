package org.hibernate.orm.test.type.contributor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.BindingContext;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType implements UserType<Array>, BindableType<Array>, BasicValueConverter<Array, String> {
    public static final ArrayType INSTANCE = new ArrayType();

    private final BasicJavaType<Array> javaType = ArrayJavaType.INSTANCE;
    private final JdbcType jdbcType = VarcharJdbcType.INSTANCE;

    @Override
    public Class<Array> getBindableJavaType() {
        // really a UserType should not implement BindableType
        return Array.class;
    }

    @Override
    public SqmExpressible<Array> resolveExpressible(BindingContext bindingContext) {
        // really a UserType should not implement BindableType
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSqlType() {
        return jdbcType.getJdbcTypeCode();
    }

    @Override
    public JdbcType getJdbcType(TypeConfiguration typeConfiguration) {
        return jdbcType;
    }

    @Override
    public Class<Array> returnedClass() {
        return Array.class;
    }

    @Override
    public boolean equals(Array x, Array y) throws HibernateException {
        return javaType.areEqual( x, y );
    }

    @Override
    public int hashCode(Array x) throws HibernateException {
        return javaType.extractHashCode( x );
    }

    @Override
    public Array nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session)
            throws SQLException {
        return jdbcType.getExtractor( javaType ).extract( rs, position, session );
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Array value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        jdbcType.getBinder( javaType ).bind( st, value, index, session );
    }

    @Override
    public BasicValueConverter<Array, Object> getValueConverter() {
        return (BasicValueConverter) this;
    }

    @Override
    public Array toDomainValue(String relationalForm) {
        return assemble( relationalForm, null );
    }

    @Override
    public String toRelationalValue(Array domainForm) {
        return (String) disassemble( domainForm );
    }

    @Override
    public JavaType<Array> getDomainJavaType() {
        return javaType;
    }

    @Override
    public JavaType<String> getRelationalJavaType() {
        return StringJavaType.INSTANCE;
    }

    @Override
    public Array deepCopy(Array value) throws HibernateException {
        return ArrayMutabilityPlan.INSTANCE.deepCopy( value );
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Array value) throws HibernateException {
        return ArrayMutabilityPlan.INSTANCE.disassemble( value, null );
    }

    @Override
    public Array assemble(Serializable cached, Object owner) throws HibernateException {
        return ArrayMutabilityPlan.INSTANCE.assemble( cached, null );
    }

    @Override
    public Array replace(Array original, Array target, Object owner) throws HibernateException {
        return null;
    }
}
