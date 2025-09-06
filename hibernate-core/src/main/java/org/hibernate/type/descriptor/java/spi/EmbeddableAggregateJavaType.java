/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.internal.DelayedStructJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

/**
 * Java type for embeddable aggregates, which allows resolving a recommended {@link JdbcType}.
 *
 * @author Christian Beikov
 */
public class EmbeddableAggregateJavaType<T> extends AbstractClassJavaType<T> {

	private final String structName;

	public EmbeddableAggregateJavaType(Class<T> type, String structName) {
		super( type );
		this.structName = structName;
	}

	public String getStructName() {
		return structName;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		final var basicType = context.getTypeConfiguration().getBasicTypeForJavaType( getJavaType() );
		if ( basicType != null ) {
			return basicType.getJdbcType();
		}
		if ( structName != null ) {
			final var jdbcTypeRegistry = context.getTypeConfiguration().getJdbcTypeRegistry();
			final var aggregateDescriptor = jdbcTypeRegistry.findAggregateDescriptor( structName );
			if ( aggregateDescriptor != null ) {
				return aggregateDescriptor;
			}
			if ( jdbcTypeRegistry.findDescriptor( SqlTypes.STRUCT ) != null ) {
				return new DelayedStructJdbcType( this, structName );
			}
		}
		// When the column is mapped as XML array, the component type must be SQLXML
		final Integer explicitJdbcTypeCode = context.getExplicitJdbcTypeCode();
		if ( explicitJdbcTypeCode != null && explicitJdbcTypeCode == SqlTypes.XML_ARRAY
				// Also prefer XML as the Dialect prefers XML arrays
				|| context.getDialect().getPreferredSqlTypeCodeForArray() == SqlTypes.XML_ARRAY ) {
			final var descriptor = context.getJdbcType( SqlTypes.SQLXML );
			if ( descriptor != null ) {
				return descriptor;
			}
		}
		else {
			// Otherwise use json by default for now
			final var descriptor = context.getJdbcType( SqlTypes.JSON );
			if ( descriptor != null ) {
				return descriptor;
			}
		}
		throw new JdbcTypeRecommendationException(
				"Could not determine recommended JdbcType for `" + getTypeName() + "`"
		);
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}

	@Override
	public T fromString(CharSequence string) {
		throw new UnsupportedOperationException(
				"Conversion from String strategy not known for this Java type: " + getTypeName()
		);
	}

	@Override
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( type.isAssignableFrom( getJavaTypeClass() ) ) {
			//noinspection unchecked
			return (X) value;
		}
		throw new UnsupportedOperationException(
				"Unwrap strategy not known for this Java type: " + getTypeName()
		);
	}

	@Override
	public <X> T wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( getJavaTypeClass().isInstance( value ) ) {
			//noinspection unchecked
			return (T) value;
		}
		throw new UnsupportedOperationException(
				"Wrap strategy not known for this Java type: " + getTypeName()
		);
	}

	@Override
	public String toString() {
		return "BasicJavaType(" + getTypeName() + ")";
	}
}
