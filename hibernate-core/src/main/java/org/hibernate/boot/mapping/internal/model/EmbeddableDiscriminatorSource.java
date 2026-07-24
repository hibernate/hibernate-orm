/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;

import jakarta.annotation.Nullable;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;

/// Path-specific source facts for a polymorphic embeddable discriminator.
///
/// The contribution owns subtype discovery, discriminator values, hierarchy
/// links, and the effective discriminator-column source.  Materialization turns
/// these facts into the mutable `BasicValue` and `Component` compatibility
/// state.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddableDiscriminatorSource(
		Map<Object, String> discriminatorValues,
		Map<String, String> subclassToSuperclass,
		@Nullable ColumnSource overrideColumnSource,
		@Nullable DiscriminatorColumn discriminatorColumn) {
	public EmbeddableDiscriminatorSource {
		discriminatorValues = Collections.unmodifiableMap( new LinkedHashMap<>( discriminatorValues ) );
		subclassToSuperclass = Collections.unmodifiableMap( new LinkedHashMap<>( subclassToSuperclass ) );
	}

	public static EmbeddableDiscriminatorSource from(
			ComponentSource componentSource,
			BindingContext bindingContext) {
		final Map<Object, String> discriminatorValues = new LinkedHashMap<>();
		final Map<String, String> subclassToSuperclass = new LinkedHashMap<>();
		collectDiscriminatorValue( componentSource.componentType(), discriminatorValues );
		collectPersistentSuperclassLinks( componentSource.componentType(), subclassToSuperclass );
		final List<ClassDetails> subtypes = collectConcreteComponentSubtypes( componentSource, bindingContext );
		subtypes.sort( Comparator
				.comparingInt( (ClassDetails subtype) -> hierarchyDistance( subtype, componentSource.componentType() ) )
				.thenComparing( ClassDetails::getName ) );
		subtypes.forEach( embeddableType -> {
			collectDiscriminatorValue( embeddableType, discriminatorValues );
			collectPersistentSuperclassLinks( embeddableType, subclassToSuperclass );
		} );
		collectRuntimeSubtypeSuperclassLinks( componentSource, bindingContext, subclassToSuperclass );
		return new EmbeddableDiscriminatorSource(
				discriminatorValues,
				subclassToSuperclass,
				componentSource.discriminatorColumnSource(),
				componentSource.componentType().getDirectAnnotationUsage( DiscriminatorColumn.class )
		);
	}

	public boolean polymorphic() {
		return discriminatorValues.size() > 1;
	}

	private static List<ClassDetails> collectConcreteComponentSubtypes(
			ComponentSource componentSource,
			BindingContext bindingContext) {
		final Map<String, ClassDetails> subtypes = new LinkedHashMap<>();
		bindingContext.getCategorizedDomainModel().forEachEmbeddable( (name, embeddableType) -> {
			if ( isSubtypeOf( embeddableType, componentSource.componentType() ) ) {
				subtypes.put( className( embeddableType ), embeddableType );
			}
		} );
		return new ArrayList<>( subtypes.values() );
	}

	private static void collectRuntimeSubtypeSuperclassLinks(
			ComponentSource componentSource,
			BindingContext bindingContext,
			Map<String, String> subclassToSuperclass) {
		bindingContext.getClassDetailsRegistry().forEachClassDetails( classDetails -> {
			if ( isSubtypeOf( classDetails, componentSource.componentType() ) ) {
				collectPersistentSuperclassLinks( classDetails, subclassToSuperclass );
			}
		} );
	}

	private static void collectDiscriminatorValue(
			ClassDetails embeddableType,
			Map<Object, String> discriminatorValues) {
		final DiscriminatorValue discriminatorValue = embeddableType.getDirectAnnotationUsage( DiscriminatorValue.class );
		final String value = discriminatorValue == null || StringHelper.isBlank( discriminatorValue.value() )
				? StringHelper.unqualify( className( embeddableType ) )
				: discriminatorValue.value();
		discriminatorValues.put( value, className( embeddableType ).intern() );
	}

	private static void collectPersistentSuperclassLinks(
			ClassDetails componentType,
			Map<String, String> subclassToSuperclass) {
		for ( ClassDetails current = componentType; current != null; current = current.getSuperClass() ) {
			final ClassDetails superClass = current.getSuperClass();
			if ( superClass == null || superClass == ClassDetails.OBJECT_CLASS_DETAILS ) {
				return;
			}
			subclassToSuperclass.put( className( current ), className( superClass ) );
		}
	}

	private static boolean isSubtypeOf(ClassDetails subtype, ClassDetails supertype) {
		for ( ClassDetails candidate = subtype.getSuperClass(); candidate != null; candidate = candidate.getSuperClass() ) {
			if ( className( candidate ).equals( className( supertype ) ) ) {
				return true;
			}
		}
		return false;
	}

	private static int hierarchyDistance(ClassDetails subtype, ClassDetails supertype) {
		int distance = 0;
		for ( ClassDetails candidate = subtype; candidate != null; candidate = candidate.getSuperClass() ) {
			if ( className( candidate ).equals( className( supertype ) ) ) {
				return distance;
			}
			distance++;
		}
		return Integer.MAX_VALUE;
	}

	private static String className(ClassDetails classDetails) {
		final String className = classDetails.getClassName();
		return className == null ? classDetails.getName() : className;
	}
}
