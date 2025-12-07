/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.GenericsHelper;
import org.hibernate.models.spi.MemberDetails;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.classmate.members.ResolvedMethod;
import jakarta.persistence.AttributeConverter;

import static org.hibernate.internal.util.type.PrimitiveWrappers.canonicalize;

/**
 * Helpers related to handling converters
 */
public class ConverterHelper {
	public static ParameterizedType extractAttributeConverterParameterizedType(Class<? extends AttributeConverter<?,?>> base) {
		return GenericsHelper.extractParameterizedType( base, AttributeConverter.class );
	}

	public static ResolvedType resolveAttributeType(MemberDetails memberDetails, MetadataBuildingContext context) {
		return resolveMember( memberDetails, context ).getType();
	}

	public static ResolvedMember<? extends Member> resolveMember(MemberDetails memberDetails, MetadataBuildingContext buildingContext) {
		final var classmateContext = buildingContext.getBootstrapContext().getClassmateContext();
		final var declaringClassType =
				classmateContext.getTypeResolver()
						.resolve( memberDetails.getDeclaringType().toJavaClass() );
		final var declaringClassWithMembers =
				classmateContext.getMemberResolver()
						.resolve( declaringClassType, null, null );

		final var member = memberDetails.toJavaMember();
		if ( member instanceof Method ) {
			for ( ResolvedMethod resolvedMember : declaringClassWithMembers.getMemberMethods() ) {
				if ( resolvedMember.getName().equals( member.getName() ) ) {
					return resolvedMember;
				}
			}
		}
		else if ( member instanceof Field ) {
			for ( ResolvedField resolvedMember : declaringClassWithMembers.getMemberFields() ) {
				if ( resolvedMember.getName().equals( member.getName() ) ) {
					return resolvedMember;
				}
			}
		}
		else {
			throw new HibernateException( "Unexpected java.lang.reflect.Member type from org.hibernate.models.spi.MemberDetails : " + member );
		}

		throw new HibernateException(
				"Could not locate resolved type information for attribute [" + member.getName() + "] from Classmate"
		);
	}

	public static List<ResolvedType> resolveConverterClassParamTypes(
			Class<? extends AttributeConverter<?, ?>> converterClass,
			ClassmateContext context) {
		final var converterType = context.getTypeResolver().resolve( converterClass );
		final var converterParamTypes = converterType.typeParametersFor( AttributeConverter.class );
		if ( converterParamTypes == null ) {
			throw new AnnotationException(
					"Could not extract type argument from attribute converter class '"
							+ converterClass.getName() + "'"
			);
		}
		else if ( converterParamTypes.size() != 2 ) {
			throw new AnnotationException(
					"Unexpected type argument for attribute converter class '"
							+ converterClass.getName()
							+ "' (expected 2 type arguments, but found " + converterParamTypes.size() + ")"
			);
		}
		return converterParamTypes;
	}

	/**
	 * Determine whether 2 types match.  Intended for determining whether to auto applying a converter
	 *
	 * @param converterDefinedType The type defined via the converter's parameterized type signature.
	 * 		E.g. {@code O} in {@code implements AttributeConverter<O,R>}
	 * @param checkType The type from the domain model (basic attribute type, Map key type, Collection element type)
	 *
	 * @return {@code true} if they match, otherwise {@code false}.
	 */
	public static boolean typesMatch(ResolvedType converterDefinedType, ResolvedType checkType) {
		final var erasedCheckType = canonicalize( checkType.getErasedType() );
		if ( erasedCheckType.isArray() ) {
			// converterDefinedType have type parameters if it extends super generic class
			// but checkType doesn't have any type parameters
			// comparing erased type is enough
			// see https://hibernate.atlassian.net/browse/HHH-18012
			return converterDefinedType.getErasedType() == erasedCheckType;
		}

		return converterDefinedType.getErasedType().isAssignableFrom( erasedCheckType )
			&& checkTypeParametersMatch( converterDefinedType, checkType );
	}

	private static boolean checkTypeParametersMatch(ResolvedType converterDefinedType, ResolvedType checkType) {
		final var converterTypeParameters = converterDefinedType.getTypeParameters();
		// if the converter did not define any nested type parameters,
		// then the checks already done above are enough for a match
		if ( converterTypeParameters.isEmpty() ) {
			return true;
		}
		else {
			// However, here the converter *did* define nested type parameters,
			// so we'd have a converter defined using something like, for example,
			// List<String> for its domain type, and so we need to check those
			// nested types as well
			final var checkTypeParameters = checkType.getTypeParameters();
			if ( checkTypeParameters.isEmpty() ) {
				// the domain type did not define nested type params.  a List<String> would not auto-match a List(<Object>)
				return false;
			}
			else if ( converterTypeParameters.size() != checkTypeParameters.size() ) {
				// they had different number of type params somehow.
				return false;
			}
			else {
				for ( int i = 0; i < converterTypeParameters.size(); i++ ) {
					if ( !typesMatch( converterTypeParameters.get( i ), checkTypeParameters.get( i ) ) ) {
						return false;
					}
				}
				return true;
			}
		}
	}
}
