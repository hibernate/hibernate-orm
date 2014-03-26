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
package org.hibernate.metamodel.source.internal.annotations.util;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.metamodel.reflite.spi.ArrayDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.binding.CustomSQL;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

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


	public static CustomSQL processCustomSqlAnnotation(
			DotName annotationName,
			Map<DotName, List<AnnotationInstance>> annotations,
			ClassInfo target) {
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

	public static JavaTypeDescriptor resolveCollectionElementType(
			MemberDescriptor member,
			EntityBindingContext context) {
		final AnnotationInstance annotation;
		final String targetElementName;

		final AnnotationInstance oneToManyAnnotation = member.getAnnotations().get( JPADotNames.ONE_TO_MANY );
		final AnnotationInstance manyToManyAnnotation = member.getAnnotations().get( JPADotNames.MANY_TO_MANY );
		final AnnotationInstance elementCollectionAnnotation = member.getAnnotations().get( JPADotNames.ELEMENT_COLLECTION );

		int nonNullCount = countNonNull( oneToManyAnnotation, manyToManyAnnotation, elementCollectionAnnotation );

		if ( nonNullCount > 1 ) {
			throw context.makeMappingException(
					"Attribute [" + member.toString() +
							"] may contain only one of @OneToMany, @ManyToMany, @ElementCollection"
			);
		}

		if ( oneToManyAnnotation != null ) {
			annotation = oneToManyAnnotation;
			targetElementName = "targetEntity";
		}
		else if ( manyToManyAnnotation != null ) {
			annotation = manyToManyAnnotation;
			targetElementName = "targetEntity";
		}
		else if ( elementCollectionAnnotation != null ) {
			annotation = elementCollectionAnnotation;
			targetElementName = "targetClass";
		}
		else {
			annotation = null;
			targetElementName = null;
		}

		if ( annotation != null && annotation.value( targetElementName ) != null ) {
			final String typeName = JandexHelper.getValue(
					annotation,
					targetElementName,
					String.class,
					context.getServiceRegistry().getService( ClassLoaderService.class )
			);
			return context.getJavaTypeDescriptorRepository().getType(
					context.getJavaTypeDescriptorRepository().buildName( typeName )
			);
		}

		if ( ArrayDescriptor.class.isInstance( member.getType().getErasedType() ) ) {
			return ( (ArrayDescriptor) member.getType().getErasedType() ).getComponentType();
		}

		if ( member.getType().getResolvedParameterTypes().isEmpty() ) {
			return null; // no generic at all
		}

		// for non-maps, the element type is the first resolved parameter type.
		// For maps, the second

		final JavaTypeDescriptor attributeType = member.getType().getErasedType();
		if ( context.getJavaTypeDescriptorRepository().jdkMapDescriptor().isAssignableFrom( attributeType ) ) {
			return member.getType().getResolvedParameterTypes().get( 1 );
		}
		else if ( context.getJavaTypeDescriptorRepository().jdkCollectionDescriptor().isAssignableFrom( attributeType ) ) {
			return member.getType().getResolvedParameterTypes().get( 0 );
		}

		return null;
	}

	private static int countNonNull(Object... things) {
		if ( things == null ) {
			throw new IllegalArgumentException( "Things passed to count cannot be null" );
		}

		int count = 0;
		for ( Object thing : things ) {
			if ( thing != null ) {
				count++;
			}
		}
		return count;
	}

	public static JavaTypeDescriptor resolveMapKeyType(
			MemberDescriptor member,
			EntityBindingContext localBindingContext) {
		final JavaTypeDescriptor attributeType = member.getType().getErasedType();
		final JavaTypeDescriptor jdkMapType = localBindingContext.getJavaTypeDescriptorRepository().jdkMapDescriptor();

		JavaTypeDescriptor mapKeyType = null;
		if ( jdkMapType.isAssignableFrom( attributeType )
				&& !member.getType().getResolvedParameterTypes().isEmpty()) {
			mapKeyType = member.getType().getResolvedParameterTypes().get( 0 );
		}

		return mapKeyType;
	}

	public static NaturalIdMutability determineNaturalIdMutability(
			ManagedTypeMetadata container,
			MemberDescriptor member) {
		final AnnotationInstance naturalIdAnnotation = member.getAnnotations().get( HibernateDotNames.NATURAL_ID );
		if ( naturalIdAnnotation == null ) {
			return container.getContainerNaturalIdMutability();
		}

		final boolean mutable = naturalIdAnnotation.value( "mutable" ) != null
				&& naturalIdAnnotation.value( "mutable" ).asBoolean();
		return mutable
				? NaturalIdMutability.MUTABLE
				: NaturalIdMutability.IMMUTABLE;
	}
}


