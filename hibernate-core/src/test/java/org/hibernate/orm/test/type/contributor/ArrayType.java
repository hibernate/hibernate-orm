package org.hibernate.orm.test.type.contributor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.UserType;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType implements UserType<Array>, AttributeConverter<Array, String> {

	public static final ArrayType INSTANCE = new ArrayType();

	private final BasicJavaType<Array> javaType = ArrayJavaType.INSTANCE;
	private final JdbcType jdbcType = VarcharJdbcType.INSTANCE;

	@Override
	public int getSqlType() {
		return jdbcType.getJdbcTypeCode();
	}

	@Override
	@Nonnull
	public Class<Array> returnedClass() {
		return Array.class;
	}

	@Override
	public boolean equals(@Nullable Array x, @Nullable Array y) throws HibernateException {
		return javaType.areEqual( x, y );
	}

	@Override
	public int hashCode(@Nonnull Array x) throws HibernateException {
		return javaType.extractHashCode( x );
	}

	@Override
	@Nullable
	public Array nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return jdbcType.getExtractor( javaType ).extract( rs, position, options );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable Array value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		jdbcType.getBinder( javaType ).bind( st, value, index, options );
	}

	@Override
	@Nullable
	public AttributeConverter<Array, String> getValueConverter() {
		return this;
	}

	@Override
	public String convertToDatabaseColumn(Array domainForm) {
		return (String) disassemble( domainForm );
	}

	@Override
	public Array convertToEntityAttribute(String relationalForm) {
		return assemble( relationalForm, null );
	}

	@Override
	@Nullable
	public Array deepCopy(@Nullable Array value) throws HibernateException {
		return ArrayMutabilityPlan.INSTANCE.deepCopy( value );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable Array value) throws HibernateException {
		return ArrayMutabilityPlan.INSTANCE.disassemble( value, null );
	}

	@Override
	@Nullable
	public Array assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return ArrayMutabilityPlan.INSTANCE.assemble( cached, null );
	}

	@Override
	@Nullable
	public Array replace(
			@Nullable Array original,
			@Nullable Array target,
			@Nullable Object owner) throws HibernateException {
		return null;
	}
}
