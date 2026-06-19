/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.MappingException;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.boot.mapping.internal.categorize.CategorizationLogging.CATEGORIZATION_LOGGER;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;

/// Models the inheritance relationships between the managed types that are visible
/// to categorization.
///
/// "Visible" means included in the source set used to create this state, or added
/// while handling a missing persistent superclass according to
/// {@link MissingPersistentSuperclassHandling}.  The state intentionally does not
/// describe every persistent type reachable from the classpath; it describes the
/// inheritance graph Hibernate should consider for the current set of available
/// model sources.
///
/// Non-persistent Java classes may still appear between visible persistent types in
/// the Java inheritance chain.  They are skipped when linking visible managed
/// supertypes and subtypes.  Persistent Java supertypes that are not visible are
/// handled according to {@link MissingPersistentSuperclassHandling}.
///
/// @since 9.0
/// @author Steve Ebersole
public class ManagedTypeInheritanceState {

	/// How to handle a persistent superclass that is reachable in Java inheritance
	/// but was not included in the visible managed type set.
	public enum MissingPersistentSuperclassHandling {
		/// Treat the missing persistent superclass as a mapping error.
		EXCEPTION,
		/// Warn and continue walking past the missing persistent superclass.
		WARN_AND_IGNORE,
		/// Warn, add the missing persistent superclass to the visible set, and use it
		/// as the visible supertype.
		WARN_AND_USE
	}

	private final MissingPersistentSuperclassHandling missingPersistentSuperclassHandling;
	private final Set<ClassDetails> persistentTypes;
	private final Set<ClassDetails> rootEntities;
	private final Set<ClassDetails> mappedSuperclasses;
	private final Map<ClassDetails, ClassDetails> superTypes = new HashMap<>();
	private final Map<ClassDetails, Set<ClassDetails>> subTypes = new HashMap<>();

	public ManagedTypeInheritanceState(Collection<ClassDetails> sourceTypes) {
		this( sourceTypes, MissingPersistentSuperclassHandling.EXCEPTION );
	}

	/// Creates inheritance state for the persistent types visible from the given sources.
	///
	/// Only {@linkplain Entity entities} and {@linkplain MappedSuperclass mapped superclasses}
	/// are retained from {@code sourceTypes}.  Their visible supertype/subtype links are
	/// calculated relative to that retained set.
	public ManagedTypeInheritanceState(
			Collection<ClassDetails> sourceTypes,
			MissingPersistentSuperclassHandling missingPersistentSuperclassHandling) {
		this.missingPersistentSuperclassHandling = missingPersistentSuperclassHandling;
		this.persistentTypes = collectPersistentTypes( sourceTypes );
		this.rootEntities = setOfSize( persistentTypes.size() );
		this.mappedSuperclasses = setOfSize( persistentTypes.size() );

		final List<ClassDetails> persistentTypesToProcess = new ArrayList<>( persistentTypes );
		for ( int i = 0; i < persistentTypesToProcess.size(); i++ ) {
			final ClassDetails persistentType = persistentTypesToProcess.get( i );

			if ( isMappedSuperclass( persistentType ) ) {
				mappedSuperclasses.add( persistentType );
			}

			final ClassDetails superType = findNearestPersistentSuperType( persistentType, persistentTypesToProcess );
			if ( superType != null ) {
				superTypes.put( persistentType, superType );
				subTypes.computeIfAbsent( superType, (ignore) -> new HashSet<>() ).add( persistentType );
			}
		}

		persistentTypes.forEach( (persistentType) -> {
			if ( isEntity( persistentType ) && !hasVisibleEntitySuperType( persistentType ) ) {
				rootEntities.add( persistentType );
			}
		} );
	}

	public Set<ClassDetails> getRootEntities() {
		return rootEntities;
	}

	/// Returns all visible mapped superclasses, whether they are eventually used by
	/// an entity hierarchy or not.
	public Set<ClassDetails> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	/// Returns the nearest visible persistent supertype for `classDetails`, or
	/// `null` if none exists.
	///
	/// Non-persistent Java supertypes are skipped.  Persistent Java supertypes that are
	/// not visible are handled while this state is built according to
	/// {@link MissingPersistentSuperclassHandling}.
	public ClassDetails getSuperType(ClassDetails classDetails) {
		final ClassDetails superType = superTypes.get( classDetails );
		if ( superType != null ) {
			return superType;
		}

		for ( Map.Entry<ClassDetails, ClassDetails> entry : superTypes.entrySet() ) {
			if ( sameClass( entry.getKey(), classDetails ) ) {
				return entry.getValue();
			}
		}

		return null;
	}

	/// Visits the direct visible persistent subtypes of {@code classDetails}.
	///
	/// "Direct" here is relative to the visible managed type graph, not necessarily to
	/// Java's immediate superclass relationship.
	public void forEachSubType(ClassDetails classDetails, Consumer<ClassDetails> consumer) {
		final Set<ClassDetails> directSubTypes = subTypes.get( classDetails );
		if ( directSubTypes != null ) {
			directSubTypes.forEach( consumer );
			return;
		}

		for ( Map.Entry<ClassDetails, Set<ClassDetails>> entry : subTypes.entrySet() ) {
			if ( sameClass( entry.getKey(), classDetails ) ) {
				entry.getValue().forEach( consumer );
				return;
			}
		}
	}

	private static Set<ClassDetails> collectPersistentTypes(Collection<ClassDetails> sourceTypes) {
		final Map<String, ClassDetails> persistentTypes = new LinkedHashMap<>( sourceTypes.size() );
		sourceTypes.forEach( (sourceType) -> {
			if ( isEntity( sourceType ) || isMappedSuperclass( sourceType ) ) {
				persistentTypes.putIfAbsent( typeKey( sourceType ), sourceType );
			}
		} );
		return new LinkedHashSet<>( persistentTypes.values() );
	}

	private static String typeKey(ClassDetails classDetails) {
		final String className = classDetails.getClassName();
		return className == null ? classDetails.getName() : className;
	}

	private ClassDetails findNearestPersistentSuperType(
			ClassDetails classDetails,
			List<ClassDetails> persistentTypesToProcess) {
		ClassDetails current = classDetails.getSuperClass();
		while ( current != null ) {
			final ClassDetails existingPersistentType = findPersistentType( current );
			if ( existingPersistentType != null ) {
				return existingPersistentType;
			}
			if ( isEntity( current ) || isMappedSuperclass( current ) ) {
				switch ( missingPersistentSuperclassHandling ) {
					case EXCEPTION:
						throw new MappingException( missingPersistentSuperclassMessage( current, classDetails ) );
					case WARN_AND_IGNORE:
						CATEGORIZATION_LOGGER.warnf(
								"%s; ignoring the superclass",
								missingPersistentSuperclassMessage( current, classDetails )
						);
						current = current.getSuperClass();
						continue;
					case WARN_AND_USE:
						CATEGORIZATION_LOGGER.warnf(
								"%s; using the superclass",
								missingPersistentSuperclassMessage( current, classDetails )
						);
						persistentTypes.add( current );
						persistentTypesToProcess.add( current );
						return current;
				}
			}
			current = current.getSuperClass();
		}
		return null;
	}

	private ClassDetails findPersistentType(ClassDetails candidate) {
		for ( ClassDetails persistentType : persistentTypes ) {
			if ( sameClass( persistentType, candidate ) ) {
				return persistentType;
			}
		}
		return null;
	}

	private static boolean sameClass(ClassDetails one, ClassDetails another) {
		return one == another
				|| typeKey( one ).equals( typeKey( another ) )
				|| one.getName().equals( another.getName() );
	}

	private static String missingPersistentSuperclassMessage(
			ClassDetails persistentSuperType,
			ClassDetails classDetails) {
		return "Persistent superclass `%s` of `%s` was not included in AvailableResources".formatted(
				persistentSuperType.getName(),
				classDetails.getName()
		);
	}

	private boolean hasVisibleEntitySuperType(ClassDetails classDetails) {
		ClassDetails current = getSuperType( classDetails );
		while ( current != null ) {
			if ( isEntity( current ) ) {
				return true;
			}
			current = getSuperType( current );
		}
		return false;
	}

	public static boolean isEntity(ClassDetails classDetails) {
		return classDetails.hasDirectAnnotationUsage( Entity.class );
	}

	public static boolean isMappedSuperclass(ClassDetails classDetails) {
		return classDetails.hasDirectAnnotationUsage( MappedSuperclass.class );
	}
}
