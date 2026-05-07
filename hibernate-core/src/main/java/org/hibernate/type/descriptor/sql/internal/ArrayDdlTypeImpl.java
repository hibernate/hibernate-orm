/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static java.sql.Types.ARRAY;

/**
 * @author Gavin King
 */
public class ArrayDdlTypeImpl extends DdlTypeImpl {

	private final boolean castRawElementType;

	public ArrayDdlTypeImpl(Dialect dialect, boolean castRawElementType) {
		super( ARRAY, "array", dialect );
		this.castRawElementType = castRawElementType;
	}

	@Override
	public String getCastTypeName(Size columnSize, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
		final var pluralType = (BasicPluralType<?, ?>) type;
		return dialect.getArrayTypeName(
				getElementTypeSimpleName( pluralType.getElementType(), dialect ),
				castRawIfNecessary( getArrayElementTypeName( columnSize, ddlTypeRegistry, pluralType.getElementType() ) ),
				columnSize.getArrayLength()
		);
	}

	private String getArrayElementTypeName(Size columnSize, DdlTypeRegistry ddlTypeRegistry, BasicType<?> elementType) {
		if ( elementType.getJavaTypeDescriptor() instanceof EmbeddableAggregateJavaType<?> embeddableAggregateJavaType ) {
			return embeddableAggregateJavaType.getStructName();
		}
		else {
			return ddlTypeRegistry.getDescriptor( elementType.getJdbcType().getDdlTypeCode() )
					.getCastTypeName(
							dialect.getSizeStrategy().resolveSize(
									elementType.getJdbcMapping().getJdbcType(),
									elementType.getJavaTypeDescriptor(),
									columnSize
							),
							elementType,
							ddlTypeRegistry
					);
		}
	}

	private String castRawIfNecessary(String arrayElementTypeName) {
		return castRawElementType
				? getRawTypeName( arrayElementTypeName )
				: arrayElementTypeName;
	}

	@Override
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		if ( type == null ) {
			return super.getTypeName( columnSize, null, ddlTypeRegistry );
		}
		else {
			final var pluralType = (BasicPluralType<?, ?>) type;
			final var elementType = pluralType.getElementType();
			return dialect.getArrayTypeName(
					getElementTypeSimpleName( pluralType.getElementType(), dialect ),
					ddlTypeRegistry.getTypeName(
							elementType.getJdbcType().getDdlTypeCode(),
							dialect.getSizeStrategy().resolveSize(
									elementType.getJdbcMapping().getJdbcType(),
									elementType.getJavaTypeDescriptor(),
									columnSize
							),
							elementType
					),
					columnSize.getArrayLength()
			);
		}
	}

	private static String getElementTypeSimpleName(BasicType<?> elementType, Dialect dialect) {
		final var converter = elementType.getValueConverter();
		if ( converter != null ) {
			return converter instanceof JpaAttributeConverter<?, ?> attributeConverter
					? attributeConverter.getConverterJavaType().getJavaTypeClass().getSimpleName()
					: converter.getClass().getSimpleName();
		}
		else {
			final var elementJavaType = elementType.getJavaTypeDescriptor();
			if ( elementJavaType.getJavaTypeClass().isArray() ) {
				return dialect.getArrayTypeName(
						elementJavaType.getJavaTypeClass().getComponentType().getSimpleName(),
						null,
						null
				);
			}
			else {
				final var preferredJavaTypeClass = elementType.getJdbcType().getPreferredJavaTypeClass( null );
				final String simpleName = elementJavaType.getJavaTypeClass().getSimpleName();
				if ( preferredJavaTypeClass == null || preferredJavaTypeClass == elementJavaType.getJavaTypeClass() ) {
					return simpleName;
				}
				else {
					if ( preferredJavaTypeClass.isArray() ) {
						return simpleName + dialect.getArrayTypeName(
								preferredJavaTypeClass.getComponentType().getSimpleName(),
								null,
								null
						);
					}
					else {
						return simpleName + preferredJavaTypeClass.getSimpleName();
					}
				}
			}
		}
	}

}
