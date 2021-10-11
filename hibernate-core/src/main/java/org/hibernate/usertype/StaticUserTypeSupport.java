/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class StaticUserTypeSupport<T> implements UserType<T> {
	private final BasicJavaType javaType;
	private final JdbcTypeDescriptor jdbcType;
	private final MutabilityPlan mutabilityPlan;
	private final BasicValueConverter valueConverter;

	private final int[] sqlTypes;
	private ValueExtractor jdbcValueExtractor;
	private ValueBinder jdbcValueBinder;

	public StaticUserTypeSupport(BasicJavaType javaType, JdbcTypeDescriptor jdbcType) {
		this( javaType, jdbcType, javaType.getMutabilityPlan() );
	}

	public StaticUserTypeSupport(
			BasicJavaType javaType,
			JdbcTypeDescriptor jdbcType,
			MutabilityPlan mutabilityPlan) {
		this( javaType, jdbcType, mutabilityPlan, null );
	}

	public StaticUserTypeSupport(
			BasicJavaType javaType,
			JdbcTypeDescriptor jdbcType,
			BasicValueConverter valueConverter) {
		this( javaType, jdbcType, javaType.getMutabilityPlan(), valueConverter );
	}

	public StaticUserTypeSupport(BasicJavaType javaType, JdbcTypeDescriptor jdbcType, MutabilityPlan mutabilityPlan, BasicValueConverter valueConverter) {
		this.javaType = javaType;
		this.jdbcType = jdbcType;
		this.mutabilityPlan = mutabilityPlan;
		this.valueConverter = valueConverter;

		this.sqlTypes = new int[] { jdbcType.getJdbcTypeCode() };

		this.jdbcValueExtractor = jdbcType.getExtractor( javaType );
		this.jdbcValueBinder = jdbcType.getBinder( javaType );
	}

	public BasicJavaType getJavaType() {
		return javaType;
	}

	public JdbcTypeDescriptor getJdbcType() {
		return jdbcType;
	}

	public MutabilityPlan getMutabilityPlan() {
		return mutabilityPlan;
	}

	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	public ValueExtractor getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	public ValueBinder getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public int[] sqlTypes() {
		return sqlTypes;
	}

	@Override
	public Class<T> returnedClass() {
		return javaType.getJavaTypeClass();
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		//noinspection unchecked
		return javaType.areEqual( (T) x, (T) y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		//noinspection unchecked
		return javaType.extractHashCode( (T) x );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		final Object extracted = jdbcValueExtractor.extract( rs, position, session );

		if ( valueConverter != null ) {
			return (T) valueConverter.toDomainValue( extracted );
		}

		return (T) extracted;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, T value, int index, SharedSessionContractImplementor session) throws SQLException {
		final T valueToBind;
		if ( valueConverter != null ) {
			valueToBind = (T) valueConverter.toRelationalValue( value );
		}
		else {
			valueToBind = value;
		}

		jdbcValueBinder.bind( st, valueToBind, index, session );
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		//noinspection unchecked
		return javaType.getMutabilityPlan().deepCopy( (T) value );
	}

	@Override
	public boolean isMutable() {
		return javaType.getMutabilityPlan().isMutable();
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		//noinspection unchecked
		return javaType.getMutabilityPlan().disassemble( (T) value, null );
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return javaType.getMutabilityPlan().assemble( cached, null );
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return deepCopy( original );
	}

}
