/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.AttributeConverter;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Special-use AttributeConverterDescriptor implementation for cases where the converter will never
 * be used for auto-apply.
 *
 * @author Steve Ebersole
 */
public class AttributeConverterDescriptorNonAutoApplicableImpl implements AttributeConverterDescriptor {
	private final AttributeConverter converter;

	private Class domainType;
	private Class jdbcType;

	public AttributeConverterDescriptorNonAutoApplicableImpl(AttributeConverter converter) {
		this.converter = converter;

		final Class attributeConverterClass = converter.getClass();
		final ParameterizedType attributeConverterSignature = extractAttributeConverterParameterizedType(
				attributeConverterClass
		);
		if ( attributeConverterSignature == null ) {
			throw new AssertionFailure(
					"Could not extract ParameterizedType representation of AttributeConverter definition " +
							"from AttributeConverter implementation class [" + attributeConverterClass.getName() + "]"
			);
		}

		if ( attributeConverterSignature.getActualTypeArguments().length < 2 ) {
			throw new AnnotationException(
					"AttributeConverter [" + attributeConverterClass.getName()
							+ "] did not retain parameterized type information"
			);
		}

		if ( attributeConverterSignature.getActualTypeArguments().length > 2 ) {
			throw new AnnotationException(
					"AttributeConverter [" + attributeConverterClass.getName()
							+ "] specified more than 2 parameterized types"
			);
		}

		this.domainType = extractClass( attributeConverterSignature.getActualTypeArguments()[0] );
		if ( this.domainType == null ) {
			throw new AnnotationException(
					"Could not determine domain type from given AttributeConverter [" +
							attributeConverterClass.getName() + "]"
			);
		}

		this.jdbcType = extractClass(attributeConverterSignature.getActualTypeArguments()[1]);
		if ( this.jdbcType == null ) {
			throw new AnnotationException(
					"Could not determine JDBC type from given AttributeConverter [" +
							attributeConverterClass.getName() + "]"
			);
		}
	}

	private ParameterizedType extractAttributeConverterParameterizedType(Type base) {
		if ( base != null ) {
			Class clazz = extractClass( base );
			List<Type> types = new ArrayList<Type>();
			types.add( clazz.getGenericSuperclass() );
			types.addAll( Arrays.asList( clazz.getGenericInterfaces() ) );
			for ( Type type : types ) {
				type = resolveType( type, base );
				if ( ParameterizedType.class.isInstance( type ) ) {
					final ParameterizedType parameterizedType = (ParameterizedType) type;
					if ( AttributeConverter.class.equals( parameterizedType.getRawType() ) ) {
						return parameterizedType;
					}
				}
				ParameterizedType parameterizedType = extractAttributeConverterParameterizedType( type );
				if ( parameterizedType != null ) {
					return parameterizedType;
				}
			}
		}
		return null;
	}

	private static Class extractClass(Type type) {
		if ( type instanceof Class ) {
			return (Class) type;
		}
		else if ( type instanceof ParameterizedType ) {
			return extractClass( ( (ParameterizedType) type ).getRawType() );
		}
		return null;
	}

	private static Type resolveType(Type target, Type context) {
		if ( target instanceof ParameterizedType ) {
			return resolveParameterizedType( (ParameterizedType) target, context );
		}
		else if ( target instanceof TypeVariable ) {
			return resolveTypeVariable( (TypeVariable) target, (ParameterizedType) context );
		}
		return target;
	}

	private static ParameterizedType resolveParameterizedType(final ParameterizedType parameterizedType, Type context) {
		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

		final Type[] resolvedTypeArguments = new Type[actualTypeArguments.length];
		for ( int idx = 0; idx < actualTypeArguments.length; idx++ ) {
			resolvedTypeArguments[idx] = resolveType( actualTypeArguments[idx], context );
		}
		return new ParameterizedType() {

			@Override
			public Type[] getActualTypeArguments() {
				return resolvedTypeArguments;
			}

			@Override
			public Type getRawType() {
				return parameterizedType.getRawType();
			}

			@Override
			public Type getOwnerType() {
				return parameterizedType.getOwnerType();
			}

		};
	}

	private static Type resolveTypeVariable(TypeVariable typeVariable, ParameterizedType context) {
		Class clazz = extractClass( context.getRawType() );
		TypeVariable[] typeParameters = clazz.getTypeParameters();
		for ( int idx = 0; idx < typeParameters.length; idx++ ) {
			if ( typeVariable.getName().equals( typeParameters[idx].getName() ) ) {
				return resolveType( context.getActualTypeArguments()[idx], context );
			}
		}
		return typeVariable;
	}

	private static Class extractType(TypeVariable typeVariable) {
		java.lang.reflect.Type[] boundTypes = typeVariable.getBounds();
		if ( boundTypes == null || boundTypes.length != 1 ) {
			return null;
		}

		return (Class) boundTypes[0];
	}

	@Override
	public AttributeConverter getAttributeConverter() {
		return converter;
	}

	@Override
	public Class<?> getDomainType() {
		return domainType;
	}

	@Override
	public Class<?> getJdbcType() {
		return jdbcType;
	}

	@Override
	public boolean shouldAutoApplyToAttribute(XProperty xProperty, MetadataBuildingContext context) {
		return false;
	}

	@Override
	public boolean shouldAutoApplyToCollectionElement(XProperty xProperty, MetadataBuildingContext context) {
		return false;
	}

	@Override
	public boolean shouldAutoApplyToMapKey(XProperty xProperty, MetadataBuildingContext context) {
		return false;
	}
}
