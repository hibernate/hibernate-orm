/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class StaticUserTypeSupport<T> implements UserType<T> {
	private final BasicJavaType<T> javaType;
	private final JdbcType jdbcType;
	private final MutabilityPlan<T> mutabilityPlan;
	private final BasicValueConverter<T, Object> valueConverter;

	private final ValueExtractor<Object> jdbcValueExtractor;
	private final ValueBinder<Object> jdbcValueBinder;

	public StaticUserTypeSupport(BasicJavaType<T> javaType, JdbcType jdbcType) {
		this( javaType, jdbcType, javaType.getMutabilityPlan() );
	}

	public StaticUserTypeSupport(
			BasicJavaType<T> javaType,
			JdbcType jdbcType,
			MutabilityPlan<T> mutabilityPlan) {
		this( javaType, jdbcType, mutabilityPlan, null );
	}

	public StaticUserTypeSupport(
			BasicJavaType<T> javaType,
			JdbcType jdbcType,
			BasicValueConverter<T, Object> valueConverter) {
		this( javaType, jdbcType, javaType.getMutabilityPlan(), valueConverter );
	}

	public StaticUserTypeSupport(
			BasicJavaType<T> javaType,
			JdbcType jdbcType,
			MutabilityPlan<T> mutabilityPlan,
			BasicValueConverter<T, Object> valueConverter) {
		this.javaType = javaType;
		this.jdbcType = jdbcType;
		this.mutabilityPlan = mutabilityPlan;
		this.valueConverter = valueConverter;

		//noinspection unchecked
		this.jdbcValueExtractor = jdbcType.getExtractor( (JavaType<Object>) javaType );
		//noinspection unchecked
		this.jdbcValueBinder = jdbcType.getBinder( (JavaType<Object>) javaType );
	}

	public BasicJavaType<T> getJavaType() {
		return javaType;
	}

	@Override
	public JdbcType getJdbcType(TypeConfiguration typeConfiguration) {
		return jdbcType;
	}

	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public BasicValueConverter<T, Object> getValueConverter() {
		return valueConverter;
	}

	public ValueExtractor<Object> getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	public ValueBinder<Object> getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public int getSqlType() {
		return jdbcType.getDdlTypeCode();
	}

	@Override
	public Class<T> returnedClass() {
		return javaType.getJavaTypeClass();
	}

	@Override
	public boolean equals(T x, T y) throws HibernateException {
		return javaType.areEqual( x, y );
	}

	@Override
	public int hashCode(T x) throws HibernateException {
		return javaType.extractHashCode( x );
	}

	@Override
	public T nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session)
			throws SQLException {
		final Object extracted = jdbcValueExtractor.extract( rs, position, session );

		if ( valueConverter != null ) {
			return valueConverter.toDomainValue( extracted );
		}

		//noinspection unchecked
		return (T) extracted;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, T value, int index, SharedSessionContractImplementor session)
			throws SQLException {
		final Object valueToBind;
		if ( valueConverter != null ) {
			valueToBind = valueConverter.toRelationalValue( value );
		}
		else {
			valueToBind = value;
		}

		jdbcValueBinder.bind( st, valueToBind, index, session );
	}

	@Override
	public T deepCopy(T value) throws HibernateException {
		return javaType.getMutabilityPlan().deepCopy( value );
	}

	@Override
	public boolean isMutable() {
		return javaType.getMutabilityPlan().isMutable();
	}

	@Override
	public Serializable disassemble(T value) throws HibernateException {
		return javaType.getMutabilityPlan().disassemble( value, null );
	}

	@Override
	public T assemble(Serializable cached, Object owner) throws HibernateException {
		return javaType.getMutabilityPlan().assemble( cached, null );
	}
}
