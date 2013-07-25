/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.util;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.fasterxml.classmate.members.ResolvedMember;

/**
 * Some annotation processing is identical between entity and attribute level (aka you can place the annotation on
 * entity as well as attribute level. This class tries to avoid code duplication for these cases.
 *
 * @author Hardy Ferentschik
 */
public class AnnotationParserHelper {
	
	// should not be instantiated
	private AnnotationParserHelper() {

	}

	public static CustomSQL processCustomSqlAnnotation(DotName annotationName,
			Map<DotName, List<AnnotationInstance>> annotations) {
		final AnnotationInstance sqlAnnotation = JandexHelper.getSingleAnnotation( annotations, annotationName );

		return createCustomSQL( sqlAnnotation );
	}

	public static CustomSQL processCustomSqlAnnotation(DotName annotationName,
			Map<DotName, List<AnnotationInstance>> annotations, ClassInfo target) {
		final AnnotationInstance sqlAnnotation = JandexHelper.getSingleAnnotation( annotations, annotationName, target );

		return createCustomSQL( sqlAnnotation );
	}

	public static CustomSQL createCustomSQL(AnnotationInstance customSqlAnnotation) {
		if ( customSqlAnnotation == null ) {
			return null;
		}

		final String sql = customSqlAnnotation.value( "sql" ).asString();
		final boolean isCallable = customSqlAnnotation.value( "callable" ) != null
				&& customSqlAnnotation.value( "callable" ).asBoolean();

		final ExecuteUpdateResultCheckStyle checkStyle = customSqlAnnotation.value( "check" ) == null
				? isCallable
				? ExecuteUpdateResultCheckStyle.NONE
				: ExecuteUpdateResultCheckStyle.COUNT
				: ExecuteUpdateResultCheckStyle.valueOf( customSqlAnnotation.value( "check" ).asEnum() );

		return new CustomSQL( sql, isCallable, checkStyle );
	}

	public static String determineCustomTuplizer(Map<DotName, List<AnnotationInstance>> annotations,
			AnnotationTarget target, ClassLoaderService classLoaderService){
		//tuplizer on field
		final AnnotationInstance tuplizersAnnotation = JandexHelper.getSingleAnnotation(
				annotations, HibernateDotNames.TUPLIZERS, target
		);
		final AnnotationInstance tuplizerAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				HibernateDotNames.TUPLIZER,
				target
		);
		return determineCustomTuplizer(
				tuplizersAnnotation,
				tuplizerAnnotation,
				classLoaderService
		);
	}

	public static String determineCustomTuplizer(Map<DotName, List<AnnotationInstance>> annotations,
			ClassLoaderService classLoaderService){
		return determineCustomTuplizer( annotations, null, classLoaderService );
	}

	public static String determineCustomTuplizer(
			final AnnotationInstance tuplizersAnnotation,
			final AnnotationInstance tuplizerAnnotation,
			final ClassLoaderService classLoaderService) {
		if ( tuplizersAnnotation != null ) {
			AnnotationInstance[] annotations = JandexHelper.getValue(
					tuplizersAnnotation,
					"value",
					AnnotationInstance[].class,
					classLoaderService
			);
			for ( final AnnotationInstance annotationInstance : annotations ) {
				final String impl = findTuplizerImpl( annotationInstance );
				if ( StringHelper.isNotEmpty( impl ) ) {
					return impl;
				}
			}
		}
		else if ( tuplizerAnnotation != null ) {
			final String impl = findTuplizerImpl( tuplizerAnnotation );
			if ( StringHelper.isNotEmpty( impl ) ) {
				return impl;
			}
		}
		return null;
	}

	private static String findTuplizerImpl(final AnnotationInstance tuplizerAnnotation) {
		final EntityMode mode;
		if ( tuplizerAnnotation.value( "entityModeType" ) != null ) {
			mode = EntityMode.valueOf( tuplizerAnnotation.value( "entityModeType" ).asEnum() );
		}
		else if ( tuplizerAnnotation.value( "entityMode" ) != null ) {
			mode = EntityMode.parse( tuplizerAnnotation.value( "entityMode" ).asString() );
		}
		else {
			mode = EntityMode.POJO;
		}
		return mode == EntityMode.POJO ? tuplizerAnnotation.value( "impl" ).asString() : null;
	}

	public static Class<?> resolveCollectionElementType(
			ResolvedMember resolvedMember, Map<DotName,
			List<AnnotationInstance>> annotations,
			EntityBindingContext context) {
		final AnnotationInstance annotation;
		final String targetElementName;
		if ( JandexHelper.containsSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY ) ) {
			annotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY );
			targetElementName = "targetEntity";
		}
		else if ( JandexHelper.containsSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY ) ) {
			annotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY );
			targetElementName = "targetEntity";
		}
		else if ( JandexHelper.containsSingleAnnotation( annotations, JPADotNames.ELEMENT_COLLECTION ) ) {
			annotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ELEMENT_COLLECTION );
			targetElementName = "targetClass";
		}
		else {
			annotation = null;
			targetElementName = null;
		}
		if ( annotation != null && annotation.value( targetElementName ) != null ) {
			return context.locateClassByName(
					JandexHelper.getValue( annotation, targetElementName, String.class,
							context.getServiceRegistry().getService( ClassLoaderService.class ) )
			);
		}
		if ( resolvedMember.getType().isArray() ) {
			return resolvedMember.getType().getArrayElementType().getErasedType();
		}
		if ( resolvedMember.getType().getTypeParameters().isEmpty() ) {
			return null; // no generic at all
		}
		Class<?> type = resolvedMember.getType().getErasedType();
		if ( Collection.class.isAssignableFrom( type ) ) {
			return resolvedMember.getType().getTypeParameters().get( 0 ).getErasedType();
		}
		else if ( Map.class.isAssignableFrom( type ) ) {
			return resolvedMember.getType().getTypeParameters().get( 1 ).getErasedType();
		}
		else {
			return null;
		}
	}

	public static SingularAttributeBinding.NaturalIdMutability checkNaturalId(Map<DotName, List<AnnotationInstance>> annotations) {
		final AnnotationInstance naturalIdAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				HibernateDotNames.NATURAL_ID
		);
		if ( naturalIdAnnotation == null ) {
			return SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID;
		}
		final boolean mutable = naturalIdAnnotation.value( "mutable" ) != null && naturalIdAnnotation.value( "mutable" )
				.asBoolean();
		return mutable ? SingularAttributeBinding.NaturalIdMutability.MUTABLE : SingularAttributeBinding.NaturalIdMutability.IMMUTABLE;
	}

	public static boolean isPersistentMember(Set<String> transientNames, Set<String> explicitlyConfiguredMemberNames, Member member) {
		if ( !ReflectHelper.isProperty( member ) ) {
			return false;
		}

		if ( member instanceof Field && Modifier.isStatic( member.getModifiers() ) ) {
			// static fields are no instance variables! Catches also the case of serialVersionUID
			return false;
		}

		if ( member instanceof Method && Method.class.cast( member ).getReturnType().equals( void.class ) ){
			// not a getter
			return false;
		}

		if ( transientNames.contains( member.getName() ) ) {
			return false;
		}

		return !explicitlyConfiguredMemberNames.contains( ReflectHelper.getPropertyName( member ) );

	}
}


