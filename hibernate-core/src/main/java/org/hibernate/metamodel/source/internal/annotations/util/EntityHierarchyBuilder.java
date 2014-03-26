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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.AccessType;

import org.hibernate.AnnotationException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.EntityHierarchySourceImpl;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.IdentifiableTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.MappedSuperclassTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.RootEntityTypeMetadata;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

/**
 * Given a Jandex annotation index, processes all classes with JPA relevant
 * annotations and builds a more-easily consumed forms of them as a "hierarchy"
 * representation.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class EntityHierarchyBuilder {
	private static final Logger LOG = Logger.getLogger( EntityHierarchyBuilder.class );

	private final AnnotationBindingContext bindingContext;

	private final Set<DotName> allKnownMappedSuperclassClassNames = new HashSet<DotName>();

	/**
	 * Pre-processes the annotated entities from the index and create a set of entity hierarchies which can be bound
	 * to the metamodel.
	 *
	 * @param bindingContext The binding context, giving access to needed services and information
	 *
	 * @return a set of {@code EntityHierarchySource} instances.
	 */
	public static Set<EntityHierarchySource> createEntityHierarchies(AnnotationBindingContext bindingContext) {
		return new EntityHierarchyBuilder( bindingContext ).process();
	}

	/**
	 * Constructs a EntityHierarchyBuilder.  While all calls flow into this class statically via
	 * {@link #createEntityHierarchies}, internally each call to that method creates an instance
	 * used to hold instance state representing that parse.
	 *
	 * @param bindingContext Access to needed services and information
	 */
	private EntityHierarchyBuilder(AnnotationBindingContext bindingContext) {
		this.bindingContext = bindingContext;
	}


	/**
	 * Pre-processes the annotated entities from the index and create a set of entity hierarchies which can be bound
	 * to the metamodel.
	 *
	 * @return a set of {@code EntityHierarchySource} instances.
	 */
	public Set<EntityHierarchySource> process() {
		final Set<JavaTypeDescriptor> rootEntityDescriptors = findHierarchyRootDescriptors();
		final Set<RootEntityTypeMetadata> roots = new HashSet<RootEntityTypeMetadata>();
		final Set<EntityHierarchySource> hierarchies = new HashSet<EntityHierarchySource>();

		for ( JavaTypeDescriptor rootDescriptor : rootEntityDescriptors ) {
			final AccessType defaultAccessType = determineDefaultAccessTypeForHierarchy( rootDescriptor );
			final RootEntityTypeMetadata root = new RootEntityTypeMetadata( rootDescriptor, defaultAccessType, bindingContext );
			roots.add( root );
			final EntityHierarchySourceImpl hierarchy = new EntityHierarchySourceImpl( root, determineInheritanceType( root ) );
			hierarchies.add( hierarchy );
		}

		// At this point we have built all EntityClass and MappedSuperclass instances.
		// All entities that are considered a root are grouped in the 'roots' collection
		// Additionally we know of any unprocessed MappedSuperclass classes via
		//		'allKnownMappedSuperclassClassNames' - 'processedMappedSuperclassClassNames'

		warnAboutUnusedMappedSuperclasses( roots );

		return hierarchies;
	}

	/**
	 * Collect ClassDescriptor for all "root entities"
	 * <p/>
	 * At the same time, populates allKnownMappedSuperclassClassNames based on all
	 * encountered MappedSuperclass descriptors.
	 * <p/>
	 * At the same time, makes sure that the JavaTypeDescriptorRepository is primed with all known classes
	 * according to Jandex
	 *
	 * @return JavaTypeDescriptor for all @Entity and @MappedSuperclass classes.
	 */
	private Set<JavaTypeDescriptor> findHierarchyRootDescriptors() {
		final Set<JavaTypeDescriptor> collectedDescriptors = new HashSet<JavaTypeDescriptor>();

		final JavaTypeDescriptorRepository repo = bindingContext.getJavaTypeDescriptorRepository();
		for ( ClassInfo classInfo : bindingContext.getJandexAccess().getIndex().getKnownClasses() ) {
			final JavaTypeDescriptor descriptor = repo.getType( repo.buildName( classInfo.name().toString() ) );
			if ( descriptor == null ) {
				continue;
			}

			if ( descriptor.findLocalTypeAnnotation( JPADotNames.MAPPED_SUPERCLASS ) != null ) {
				allKnownMappedSuperclassClassNames.add( classInfo.name() );
				continue;
			}

			if ( descriptor.findLocalTypeAnnotation( JPADotNames.ENTITY ) == null ) {
				continue;
			}

			if ( isRoot( descriptor ) ) {
				collectedDescriptors.add( descriptor );
			}
		}

		return collectedDescriptors;
	}

	private boolean isRoot(JavaTypeDescriptor descriptor) {
		// perform a series of opt-out checks against the super-type hierarchy

		// an entity is considered a root of the hierarchy if:
		// 		1) it has no super-types
		//		2) its super types contain no entities (MappedSuperclasses are allowed)

		if ( !ClassDescriptor.class.isInstance( descriptor ) ) {
			return true;
		}

		ClassDescriptor current = ( (ClassDescriptor) descriptor ).getSuperType();
		while ( current != null ) {
			if ( current.findLocalTypeAnnotation( JPADotNames.ENTITY ) != null ) {
				return false;
			}
			current = current.getSuperType();
		}

		// if we hit no opt-outs we have a root
		return true;
	}

	private AccessType determineDefaultAccessTypeForHierarchy(JavaTypeDescriptor root) {
		JavaTypeDescriptor current = root;
		while ( current != null ) {
			final AnnotationInstance access = current.findLocalTypeAnnotation( JPADotNames.ACCESS );
			if ( access != null ) {
				return AccessType.valueOf( access.value().asEnum() );
			}

			final Collection<AnnotationInstance> embeddedIdAnnotations = current.findLocalAnnotations(
					JPADotNames.EMBEDDED_ID
			);
			if ( CollectionHelper.isNotEmpty( embeddedIdAnnotations ) ) {
				return determineAccessTypeByIdPlacement( current, embeddedIdAnnotations );
			}

			final Collection<AnnotationInstance> idAnnotations = current.findLocalAnnotations(
					JPADotNames.ID
			);
			if ( CollectionHelper.isNotEmpty( idAnnotations ) ) {
				return determineAccessTypeByIdPlacement( current, idAnnotations );
			}

			if ( ClassDescriptor.class.isInstance( current ) ) {
				current = ( (ClassDescriptor) current ).getSuperType();
			}
			else {
				current = null;
			}
		}

		throw makeMappingException(
				"Unable to locate identifier attribute for class hierarchy to determine default AccessType",
				root
		);
	}

	private static AnnotationException makeMappingException(String message, JavaTypeDescriptor typeDescriptor) {
		return new AnnotationException(
				message,
				new Origin( SourceType.ANNOTATION, typeDescriptor.getName().toString() )
		);
	}

	private static AccessType determineAccessTypeByIdPlacement(
			JavaTypeDescriptor descriptor,
			Collection<AnnotationInstance> idAnnotations) {
		AccessType accessType = null;
		for ( AnnotationInstance annotation : idAnnotations ) {
			AccessType tmpAccessType;
			if ( annotation.target() instanceof FieldInfo ) {
				tmpAccessType = AccessType.FIELD;
			}
			else if ( annotation.target() instanceof MethodInfo ) {
				tmpAccessType = AccessType.PROPERTY;
			}
			else {
				throw makeMappingException(
						"Invalid placement of @" + annotation.name().toString() + " annotation.  Target was " +
								annotation.target() + "; expecting field or method",
						descriptor
				);
			}

			if ( accessType == null ) {
				accessType = tmpAccessType;
			}
			else {
				if ( !accessType.equals( tmpAccessType ) ) {
					throw makeMappingException(
							"Inconsistent placement of @" + annotation.name().toString() + " annotation on class",
							descriptor
					);
				}
			}
		}
		return accessType;
	}

	private InheritanceType determineInheritanceType(EntityTypeMetadata root) {
		if ( root.getSubclasses().isEmpty() ) {
			// I make an assumption here that *any* subclasses are a indicator of
			// "persistent inheritance" but that is not strictly true.  Really its any
			// subclasses that are Entity (MappedSuperclass does not count).
			return InheritanceType.NO_INHERITANCE;
		}

		InheritanceType inheritanceType = root.getLocallyDefinedInheritanceType();
		if ( inheritanceType == null ) {
			// if we have more than one entity class the default is SINGLE_TABLE
			inheritanceType = InheritanceType.SINGLE_TABLE;
		}

		// Validate that there is no @Inheritance annotation further down the hierarchy
		ensureNoInheritanceAnnotationsOnSubclasses( root );

		return inheritanceType;
	}

	private void ensureNoInheritanceAnnotationsOnSubclasses(IdentifiableTypeMetadata clazz) {
		for ( ManagedTypeMetadata subclass : clazz.getSubclasses() ) {
			final AnnotationInstance inheritanceAnnotation = subclass.getJavaTypeDescriptor()
					.findLocalTypeAnnotation( JPADotNames.INHERITANCE );
			if ( inheritanceAnnotation != null ) {
				LOG.warnf(
						"@javax.persistence.Inheritance was specified on non-root entity [%s]; ignoring...",
						clazz.getName()
				);
			}
			ensureNoInheritanceAnnotationsOnSubclasses( (IdentifiableTypeMetadata) subclass );
		}
	}

	private void warnAboutUnusedMappedSuperclasses(Set<RootEntityTypeMetadata> roots) {
		for ( RootEntityTypeMetadata root : roots ) {
			walkUp( root );
			walkDown( root );
		}

		// At this point, any left in the allKnownMappedSuperclassClassNames
		// collection are unused...
		for ( DotName mappedSuperclassName : allKnownMappedSuperclassClassNames ) {
			// todo : i18n log message?
			LOG.debugf(
					"Encountered MappedSuperclass [%s] which was unused in any entity hierarchies",
					mappedSuperclassName
			);
		}

		allKnownMappedSuperclassClassNames.clear();
	}

	private void walkUp(IdentifiableTypeMetadata type) {
		if ( type == null ) {
			return;
		}

		if ( MappedSuperclassTypeMetadata.class.isInstance( type ) ) {
			allKnownMappedSuperclassClassNames.remove( type.getJavaTypeDescriptor().getName() );
		}

		walkUp( type.getSuperType() );
	}

	private void walkDown(ManagedTypeMetadata type) {
		for ( ManagedTypeMetadata sub : type.getSubclasses() ) {
			if ( MappedSuperclassTypeMetadata.class.isInstance( sub ) ) {
				allKnownMappedSuperclassClassNames.remove( sub.getJavaTypeDescriptor().getName() );
			}

			walkDown( sub );
		}
	}
}

