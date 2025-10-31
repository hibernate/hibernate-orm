/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tuple.TupleType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectArrayJavaType;

import static jakarta.persistence.metamodel.Type.PersistenceType.EMBEDDABLE;

/**
 * @author Christian Beikov
 */
public class ArrayTupleType
		implements TupleType<Object[]>, SqmDomainType<Object[]>, MappingModelExpressible<Object[]> {

	private final ObjectArrayJavaType javaType;
	private final SqmBindableType<?>[] components;

	public ArrayTupleType(SqmBindableType<?>[] components) {
		this.components = components;
		this.javaType = new ObjectArrayJavaType( getTypeDescriptors( components ) );
	}

	@Override
	public Class<Object[]> getJavaType() {
		return TupleType.super.getJavaType();
	}

	@Override
	public String getTypeName() {
		return SqmDomainType.super.getTypeName();
	}

	@Override
	public @Nullable SqmDomainType<Object[]> getSqmType() {
		return this;
	}

	private static JavaType<?>[] getTypeDescriptors(SqmExpressible<?>[] components) {
		final JavaType<?>[] typeDescriptors = new JavaType<?>[components.length];
		for ( int i = 0; i < components.length; i++ ) {
			typeDescriptors[i] = components[i].getExpressibleJavaType();
		}
		return typeDescriptors;
	}

	@Override
	public int componentCount() {
		return components.length;
	}

	@Override
	public String getComponentName(int index) {
		throw new UnsupportedMappingException( "Array tuple has no component names" );
	}

	@Override
	public List<String> getComponentNames() {
		throw new UnsupportedMappingException( "Array tuple has no component names" );
	}

	@Override
	public SqmBindableType<?> get(int index) {
		return components[index];
	}

	@Override
	public SqmBindableType<?> get(String componentName) {
		throw new UnsupportedMappingException( "Array tuple has no component names" );
	}

	@Override
	public JavaType<Object[]> getExpressibleJavaType() {
		return javaType;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return EMBEDDABLE;
	}

	@Override
	public String toString() {
		return "ArrayTupleType" + Arrays.toString( components );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		throw new UnsupportedOperationException();
	}
}
