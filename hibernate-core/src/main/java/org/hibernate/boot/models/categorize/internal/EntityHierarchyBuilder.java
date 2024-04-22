/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityHierarchyCollection;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Builds {@link EntityHierarchy} references from
 * {@linkplain ClassDetailsRegistry#forEachClassDetails managed classes}.
 *
 * @author Steve Ebersole
 */
public class EntityHierarchyBuilder {

	/**
	 * Pre-processes the annotated entities from the index and create a set of entity hierarchies which can be bound
	 * to the metamodel.
	 *
	 * @param typeConsumer Callback for any identifiable-type metadata references
	 * @param buildingContext The table context, giving access to needed services and information
	 *
	 * @return a set of {@code EntityHierarchySource} instances.
	 */
	public static EntityHierarchyCollection createEntityHierarchies(
			Set<ClassDetails> rootEntities,
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext buildingContext) {
		return new EntityHierarchyBuilder( buildingContext ).process( rootEntities, typeConsumer );
	}

	/**
	 * Pre-processes the annotated entities from the index and create a set of entity hierarchies which can be bound
	 * to the metamodel.
	 *
	 * @param typeConsumer Callback for any identifiable-type metadata references
	 * @param buildingContext The table context, giving access to needed services and information
	 *
	 * @return a set of {@code EntityHierarchySource} instances.
	 */
	public static EntityHierarchyCollection createEntityHierarchies(
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext buildingContext) {
		return createEntityHierarchies(
				collectRootEntityTypes( buildingContext.getClassDetailsRegistry() ),
				typeConsumer,
				buildingContext
		);
	}

	private final ModelCategorizationContext modelContext;

	public EntityHierarchyBuilder(ModelCategorizationContext modelContext) {
		this.modelContext = modelContext;
	}

	private EntityHierarchyCollection process(
			Set<ClassDetails> rootEntities,
			HierarchyTypeConsumer typeConsumer) {
		final Set<EntityHierarchy> hierarchies = CollectionHelper.setOfSize( rootEntities.size() );

		rootEntities.forEach( (rootEntity) -> {
			final AccessType defaultAccessType = determineDefaultAccessTypeForHierarchy( rootEntity );
			hierarchies.add( new EntityHierarchyImpl(
					rootEntity,
					defaultAccessType,
					org.hibernate.cache.spi.access.AccessType.TRANSACTIONAL,
					typeConsumer,
					modelContext
			) );
		} );

		return new EntityHierarchyCollectionImpl( hierarchies );
	}

	private AccessType determineDefaultAccessTypeForHierarchy(ClassDetails rootEntityType) {
		assert rootEntityType != null;

		// look for `@Id` or `@EmbeddedId`
		// todo (7.0) : technically we could probably look for member with any "mapping" annotation
		return resolveDefaultAccessTypeFromMembers( rootEntityType );
	}

	private AccessType resolveDefaultAccessTypeFromMembers(ClassDetails rootEntityType) {
		ClassDetails current = rootEntityType;
		while ( current != null ) {
			// look for `@Id` or `@EmbeddedId` (w/o `@Access`)
			final AnnotationTarget idMember = determineIdMember( current );
			if ( idMember != null ) {
				return switch ( idMember.getKind() ) {
					case FIELD -> AccessType.FIELD;
					case METHOD -> AccessType.PROPERTY;
					default -> throw new IllegalStateException(
							"@Id / @EmbeddedId found on target other than field or method : " + idMember );
				};
			}

			current = current.getSuperClass();

			// only consider managed classes
			while ( current != null && !isManagedClass( current ) ) {
				current = current.getSuperClass();
			}
		}

		return null;
	}

	private boolean isManagedClass(ClassDetails current) {
		return current.hasAnnotationUsage( Entity.class )
				|| current.hasAnnotationUsage( MappedSuperclass.class )
				|| current.hasAnnotationUsage( Embeddable.class );
	}

	private AnnotationTarget determineIdMember(ClassDetails current) {
		final AnnotationUsage<Access> accessUsage = current.getAnnotationUsage( Access.class );
		final AccessType explicitClassAccessType = accessUsage == null
				? null
				: accessUsage.getEnum( "value" );

		final List<FieldDetails> fields = current.getFields();
		for ( int i = 0; i < fields.size(); i++ ) {
			final FieldDetails fieldDetails = fields.get( i );
			if ( canDefineDefaultAccessType( fieldDetails, current, explicitClassAccessType ) ) {
				return fieldDetails;
			}
		}

		final List<MethodDetails> methods = current.getMethods();
		for ( int i = 0; i < methods.size(); i++ ) {
			final MethodDetails methodDetails = methods.get( i );
			if ( canDefineDefaultAccessType( methodDetails, current, explicitClassAccessType ) ) {
				return methodDetails;
			}
		}

		return null;
	}

	/**
	 * Whether the given member is capable of implying the default access type
	 */
	private boolean canDefineDefaultAccessType(
			MemberDetails memberDetails,
			ClassDetails classDetails,
			AccessType explicitClassAccessType) {
		// only persistable members can define default access type
		if ( !memberDetails.isPersistable() ) {
			return false;
		}

		return memberDetails.hasAnnotationUsage( Id.class )
				|| memberDetails.hasAnnotationUsage( EmbeddedId.class );
	}

	private Set<ClassDetails> collectRootEntityTypes() {
		return collectRootEntityTypes( modelContext.getClassDetailsRegistry() );
	}

	private static Set<ClassDetails> collectRootEntityTypes(ClassDetailsRegistry classDetailsRegistry) {
		final Set<ClassDetails> collectedTypes = new HashSet<>();

		classDetailsRegistry.forEachClassDetails( (managedType) -> {
			if ( managedType.getAnnotationUsage( JpaAnnotations.ENTITY ) != null
					&& isRoot( managedType ) ) {
				collectedTypes.add( managedType );
			}
		} );

		return collectedTypes;
	}

	public static boolean isRoot(ClassDetails classInfo) {
		// perform a series of opt-out checks against the super-type hierarchy

		// an entity is considered a root of the hierarchy if either:
		// 		1) it has no super-types
		//		2) its super types contain no entities (MappedSuperclasses are allowed)

		if ( classInfo.getSuperClass() == null ) {
			return true;
		}

		// todo (7.0) : alternatively consider an approach of tracking roots here as we walk the supers
		//		to avoid potentially walking the same supers multiple times (from diff subs)
		//		- see `#collectRootEntityTypes`

		ClassDetails current = classInfo.getSuperClass();
		while (  current != null ) {
			if ( current.getAnnotationUsage( Entity.class ) != null ) {
				// a super type has `@Entity` -> classInfo cannot be a root entity
				return false;
			}
			current = current.getSuperClass();
		}

		// if we hit no opt-outs we have a root
		return true;
	}


	/**
	 * Used in tests
	 */
	public static EntityHierarchyCollection createEntityHierarchies(ModelCategorizationContext processingContext) {
		return new EntityHierarchyBuilder( processingContext ).process(
				collectRootEntityTypes( processingContext.getClassDetailsRegistry() ),
				EntityHierarchyBuilder::ignore
		);
	}

	private static void ignore(IdentifiableTypeMetadata it) {}
}
