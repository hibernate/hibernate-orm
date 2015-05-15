/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.AttributeConverter;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class AttributeConverterDefinition {
	private static final Logger log = Logger.getLogger( AttributeConverterDefinition.class );

	private final AttributeConverter attributeConverter;
	private final boolean autoApply;
	private final Class entityAttributeType;
	private final Class databaseColumnType;

	public AttributeConverterDefinition(AttributeConverter attributeConverter, boolean autoApply) {
		this.attributeConverter = attributeConverter;
		this.autoApply = autoApply;

		final Class attributeConverterClass = attributeConverter.getClass();
		final ParameterizedType attributeConverterSignature = extractAttributeConverterParameterizedType( attributeConverterClass );
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
		entityAttributeType = extractClass( attributeConverterSignature.getActualTypeArguments()[0] );
		if ( entityAttributeType == null ) {
			throw new AnnotationException(
					"Could not determine 'entity attribute' type from given AttributeConverter [" +
							attributeConverterClass.getName() + "]"
			);
		}

		databaseColumnType = extractClass(attributeConverterSignature.getActualTypeArguments()[1]);
		if ( databaseColumnType == null ) {
			throw new AnnotationException(
					"Could not determine 'database column' type from given AttributeConverter [" +
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

	public AttributeConverter getAttributeConverter() {
		return attributeConverter;
	}

	public boolean isAutoApply() {
		return autoApply;
	}

	public Class getEntityAttributeType() {
		return entityAttributeType;
	}

	public Class getDatabaseColumnType() {
		return databaseColumnType;
	}

	private static Class extractType(TypeVariable typeVariable) {
		java.lang.reflect.Type[] boundTypes = typeVariable.getBounds();
		if ( boundTypes == null || boundTypes.length != 1 ) {
			return null;
		}

		return (Class) boundTypes[0];
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

	@Override
	public String toString() {
		return String.format(
				"%s[converterClass=%s, domainType=%s, jdbcType=%s]",
				this.getClass().getName(),
				attributeConverter.getClass().getName(),
				entityAttributeType.getName(),
				databaseColumnType.getName()
		);
	}
}
