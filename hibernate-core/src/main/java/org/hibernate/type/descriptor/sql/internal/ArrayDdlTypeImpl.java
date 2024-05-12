/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.JavaType;
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
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) type;
		final BasicType<?> elementType = pluralType.getElementType();
		String arrayElementTypeName;
		if ( elementType.getJavaTypeDescriptor() instanceof EmbeddableAggregateJavaType<?> ) {
			arrayElementTypeName = ( (EmbeddableAggregateJavaType<?>) elementType.getJavaTypeDescriptor() ).getStructName();
		}
		else {
			arrayElementTypeName = ddlTypeRegistry.getDescriptor( elementType.getJdbcType().getDdlTypeCode() )
					.getCastTypeName(
							dialect.getSizeStrategy().resolveSize(
									elementType.getJdbcMapping().getJdbcType(),
									elementType.getJavaTypeDescriptor(),
									columnSize.getPrecision(),
									columnSize.getScale(),
									columnSize.getLength()
							),
							elementType,
							ddlTypeRegistry
					);
		}
		if ( castRawElementType ) {
			final int paren = arrayElementTypeName.indexOf( '(' );
			if ( paren > 0 ) {
				final int parenEnd = arrayElementTypeName.lastIndexOf( ')' );
				arrayElementTypeName = parenEnd + 1 == arrayElementTypeName.length()
						? arrayElementTypeName.substring( 0, paren )
						: ( arrayElementTypeName.substring( 0, paren ) + arrayElementTypeName.substring( parenEnd + 1 ) );
			}
		}
		return dialect.getArrayTypeName(
				getElementTypeSimpleName( pluralType.getElementType(), dialect ),
				arrayElementTypeName,
				columnSize.getArrayLength()
		);
	}

	@Override
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) type;
		final BasicType<?> elementType = pluralType.getElementType();
		final String arrayElementTypeName = ddlTypeRegistry.getTypeName(
				elementType.getJdbcType().getDdlTypeCode(),
				dialect.getSizeStrategy().resolveSize(
						elementType.getJdbcMapping().getJdbcType(),
						elementType.getJavaTypeDescriptor(),
						columnSize.getPrecision(),
						columnSize.getScale(),
						columnSize.getLength()
				),
				elementType
		);
		return dialect.getArrayTypeName(
				getElementTypeSimpleName( pluralType.getElementType(), dialect ),
				arrayElementTypeName,
				columnSize.getArrayLength()
		);
	}

	private static String getElementTypeSimpleName(BasicType<?> elementType, Dialect dialect) {
		final BasicValueConverter<?, ?> converter = elementType.getValueConverter();
		if ( converter != null ) {
			if ( converter instanceof JpaAttributeConverter<?, ?> ) {
				return ( (JpaAttributeConverter<?, ?>) converter ).getConverterJavaType()
						.getJavaTypeClass()
						.getSimpleName();
			}
			else {
				return converter.getClass().getSimpleName();
			}
		}
		final JavaType<?> elementJavaType = elementType.getJavaTypeDescriptor();
		if ( elementJavaType.getJavaTypeClass().isArray() ) {
			return dialect.getArrayTypeName(
					elementJavaType.getJavaTypeClass().getComponentType().getSimpleName(),
					null,
					null
			);
		}
		else {
			final Class<?> preferredJavaTypeClass = elementType.getJdbcType().getPreferredJavaTypeClass( null );
			if ( preferredJavaTypeClass == null || preferredJavaTypeClass == elementJavaType.getJavaTypeClass() ) {
				return elementJavaType.getJavaTypeClass().getSimpleName();
			}
			else {
				if ( preferredJavaTypeClass.isArray() ) {
					return elementJavaType.getJavaTypeClass().getSimpleName() + dialect.getArrayTypeName(
							preferredJavaTypeClass.getComponentType().getSimpleName(),
							null,
							null
					);
				}
				else {
					return elementJavaType.getJavaTypeClass().getSimpleName() + preferredJavaTypeClass.getSimpleName();
				}
			}
		}
	}

}
