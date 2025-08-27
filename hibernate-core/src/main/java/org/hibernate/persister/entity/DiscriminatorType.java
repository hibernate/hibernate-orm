/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.type.AbstractType;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.MappingContext;

/**
 * @deprecated The functionality of DiscriminatorType, {@link DiscriminatorMetadata} and {@link MetaType} have been
 * consolidated into {@link EntityDiscriminatorMapping} and {@link DiscriminatorConverter}
 *
 * @author Steve Ebersole
 */
@Internal
@Deprecated( since = "6.2", forRemoval = true )
public class DiscriminatorType<T> extends AbstractType implements BasicType<T> {
	private final BasicType<Object> underlyingType;
	private final EntityPersister persister;
	private final DiscriminatorConverter converter;

	public DiscriminatorType(
			BasicType<?> underlyingType,
			EntityPersister persister,
			DiscriminatorConverter converter) {
		this.underlyingType = (BasicType<Object>) underlyingType;
		this.persister = persister;
		this.converter = converter;
	}

	public BasicType<?> getUnderlyingType() {
		return underlyingType;
	}

	@Override
	public DiscriminatorConverter getValueConverter() {
		return converter;
	}

	@Override
	public JavaType<?> getJdbcJavaType() {
		return underlyingType.getJdbcJavaType();
	}

	@Override
	public Class<?> getReturnedClass() {
		return Class.class;
	}

	@Override
	public Class getJavaType() {
		return Class.class;
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public T extract(CallableStatement statement, int paramIndex, SharedSessionContractImplementor session)
			throws SQLException {
		final Object discriminatorValue = underlyingType.extract( statement, paramIndex, session );
		//noinspection unchecked
		return (T) converter.toDomainValue( discriminatorValue );
	}

	@Override
	public T extract(CallableStatement statement, String paramName, SharedSessionContractImplementor session)
			throws SQLException {
		final Object discriminatorValue = underlyingType.extract( statement, paramName, session );
		//noinspection unchecked
		return (T) converter.toDomainValue( discriminatorValue );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		nullSafeSet( st, value, index, session );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		//noinspection unchecked
		final Object relationalValue = converter.toRelationalValue( value );
		underlyingType.nullSafeSet( st, relationalValue, index, session);
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return value == null ? "[null]" : value.toString();
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return value;
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache) {
		return original;
	}

	@Override
	public boolean[] toColumnNullness(Object value, MappingContext mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return Objects.equals( old, current );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		//noinspection unchecked
		return converter.toRelationalValue( value );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		underlyingType.addToCacheKey( cacheKey, value, session );
	}

	// simple delegation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public int[] getSqlTypeCodes(MappingContext mappingContext) throws MappingException {
		return underlyingType.getSqlTypeCodes( mappingContext );
	}

	@Override
	public int getColumnSpan(MappingContext mapping) throws MappingException {
		return underlyingType.getColumnSpan( mapping );
	}

	@Override
	public boolean canDoExtraction() {
		return underlyingType.canDoExtraction();
	}

	@SuppressWarnings("unchecked")
	@Override
	public JavaType<T> getExpressibleJavaType() {
		return (JavaType<T>) (persister.getRepresentationStrategy().getMode() == RepresentationMode.POJO
				? ClassJavaType.INSTANCE
				: StringJavaType.INSTANCE);
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return this.getExpressibleJavaType();
	}

	@Override
	public JavaType<T> getMappedJavaType() {
		return this.getExpressibleJavaType();
	}

	@Override
	public JdbcType getJdbcType() {
		return underlyingType.getJdbcType();
	}

	@Override
	public ValueExtractor<T> getJdbcValueExtractor() {
		return (ValueExtractor<T>) underlyingType.getJdbcValueExtractor();
	}

	@Override
	public ValueBinder<T> getJdbcValueBinder() {
		return (ValueBinder<T>) underlyingType.getJdbcValueBinder();
	}

	@Override
	public JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return underlyingType.getJdbcLiteralFormatter();
	}

	@Override
	public String[] getRegistrationKeys() {
		return ArrayHelper.EMPTY_STRING_ARRAY;
	}
}
