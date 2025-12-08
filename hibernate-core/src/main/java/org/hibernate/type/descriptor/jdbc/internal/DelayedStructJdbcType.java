/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.StructuredJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#STRUCT STRUCT} handling, which is only a temporary placeholder.
 * During bootstrap, {@link EmbeddableAggregateJavaType} will report {@link DelayedStructJdbcType} as recommended
 * {@link org.hibernate.type.descriptor.jdbc.JdbcType}, because the real {@link StructuredJdbcType} can only be built later,
 * as that requires runtime model information in the form of {@link EmbeddableMappingType}.
 * The real {@link StructuredJdbcType} is built right after {@link EmbeddableMappingType} is created,
 * which will then cause a rebuild of the respective {@link org.hibernate.type.BasicType} as well as updating
 * the {@link org.hibernate.mapping.BasicValue.Resolution} of the owning attribute.
 *
 * @see EmbeddableAggregateJavaType
 */
public class DelayedStructJdbcType implements StructuredJdbcType {

	private final EmbeddableAggregateJavaType<?> embeddableAggregateJavaType;
	private final String structName;

	public DelayedStructJdbcType(EmbeddableAggregateJavaType<?> embeddableAggregateJavaType, String structName) {
		this.embeddableAggregateJavaType = embeddableAggregateJavaType;
		this.structName = structName;
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.STRUCT;
	}

	@Override
	public String getStructTypeName() {
		return structName;
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return embeddableAggregateJavaType;
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return embeddableAggregateJavaType.getJavaTypeClass();
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return null;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return null;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return null;
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return null;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFriendlyName() {
		return "STRUCT";
	}

	@Override
	public String toString() {
		return "UnresolvedStructTypeDescriptor";
	}

	@Override
	public boolean equals(Object o) {
		return o != null
			&& getClass() == o.getClass()
			&& structName.equals( ( (DelayedStructJdbcType) o ).structName );
	}

	@Override
	public int hashCode() {
		return structName.hashCode();
	}
}
