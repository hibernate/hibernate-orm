/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.model.internal.CannotForceNonNullableException;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Version;

import static org.hibernate.boot.mapping.internal.categorize.CategorizationLogging.CATEGORIZATION_LOGGER;

/// Used to collect useful details about a hierarchy as we build its metadata
///
/// @implNote Types are collected from root down.  We make use
/// of that detail in a number of places here in the code.
///
/// @since 9.0
/// @author Steve Ebersole
public class HierarchyMetadataCollector {
	private final EntityHierarchy entityHierarchy;
	private final ClassDetails rootEntityClassDetails;
	private final CategorizationContext modelContext;
	private final MappedSuperclassTracker mappedSuperclassTracker;

	private boolean belowRootEntity;

	private EntityTypeMetadata rootEntityMetadata;
	private Inheritance inheritanceAnnotation;
	private OptimisticLocking optimisticLockingAnnotation;
	private Cache cacheAnnotation;
	private NaturalIdCache naturalIdCacheAnnotation;

	private KeyMapping idMapping;
	private AttributeMetadata versionAttribute;
	private AttributeMetadata tenantIdAttribute;

	private IdClass idClassAnnotation;
	private Object collectedIdAttributes;
	private Object collectedNaturalIdAttributes;

	public HierarchyMetadataCollector(
			EntityHierarchy entityHierarchy,
			ClassDetails rootEntityClassDetails,
			CategorizationContext modelContext,
			MappedSuperclassTracker mappedSuperclassTracker) {
		this.entityHierarchy = entityHierarchy;
		this.rootEntityClassDetails = rootEntityClassDetails;
		this.modelContext = modelContext;
		this.mappedSuperclassTracker = mappedSuperclassTracker;
	}

	public EntityTypeMetadata getRootEntityMetadata() {
		return rootEntityMetadata;
	}

	public KeyMapping getIdMapping() {
		if ( idMapping == null ) {
			idMapping = buildIdMapping();
		}

		return idMapping;
	}

	public Inheritance getInheritanceAnnotation() {
		return inheritanceAnnotation;
	}

	public AttributeMetadata getVersionAttribute() {
		return versionAttribute;
	}

	public AttributeMetadata getTenantIdAttribute() {
		return tenantIdAttribute;
	}

	public OptimisticLocking getOptimisticLockingAnnotation() {
		return optimisticLockingAnnotation;
	}

	public Cache getCacheAnnotation() {
		return cacheAnnotation;
	}

	public NaturalIdCache getNaturalIdCacheAnnotation() {
		return naturalIdCacheAnnotation;
	}

	private KeyMapping buildIdMapping() {
		if ( idClassAnnotation != null ) {
			completeExplicitIdClassAttributes();
		}

		if ( collectedIdAttributes instanceof List ) {
			//noinspection unchecked
			final List<AttributeMetadata> idAttributes = (List<AttributeMetadata>) collectedIdAttributes;
			final ClassDetails idClassDetails;
			if ( idClassAnnotation == null ) {
				idClassDetails = resolveGeneratedIdClassDetails();
			}
			else {
				idClassDetails = classDetails( idClassAnnotation.value() );
			}
			return new NonAggregatedKeyMappingImpl( idAttributes, idClassDetails );
		}

		final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;
		if ( idAttribute == null ) {
			throw new AnnotationException(
					String.format(
							Locale.ROOT,
							"Entity `%s` did not define an identifier",
							entityHierarchy.getRoot().getEntityName()
					)
			);
		}

		if ( idClassAnnotation != null ) {
			return new NonAggregatedKeyMappingImpl(
					List.of( idAttribute ),
					classDetails( idClassAnnotation.value() )
			);
		}

		if ( idAttribute.getNature() == AttributeNature.BASIC ) {
			return new BasicKeyMappingImpl( idAttribute );
		}

		if ( idAttribute.getNature() == AttributeNature.EMBEDDED ) {
			return new AggregatedKeyMappingImpl( idAttribute );
		}

		if ( idAttribute.getNature() == AttributeNature.TO_ONE ) {
			return new NonAggregatedKeyMappingImpl( List.of( idAttribute ), null );
		}

		throw new ModelsException(
				String.format(
						Locale.ROOT,
						"Unexpected attribute nature [%s] - %s",
						idAttribute.getNature(),
						entityHierarchy.getRoot().getEntityName()
				)
		);
	}

	private void completeExplicitIdClassAttributes() {
		if ( rootEntityMetadata == null ) {
			return;
		}
		final ClassDetails idClassDetails = classDetails( idClassAnnotation.value() );
		final List<String> idClassMemberNames = idClassMemberNames( idClassDetails );
		if ( idClassMemberNames.isEmpty() ) {
			return;
		}

		rootEntityMetadata.forEachAttribute( (index, attributeMetadata) -> {
			if ( idClassMemberNames.contains( attributeMetadata.getName() )
					&& !hasCollectedIdAttribute( attributeMetadata.getName() ) ) {
				collectIdAttribute( attributeMetadata );
			}
		} );
	}

	@SuppressWarnings("unchecked")
	private boolean hasCollectedIdAttribute(String attributeName) {
		if ( collectedIdAttributes == null ) {
			return false;
		}
		if ( collectedIdAttributes instanceof AttributeMetadata attributeMetadata ) {
			return attributeName.equals( attributeMetadata.getName() );
		}
		//noinspection unchecked
		for ( AttributeMetadata attributeMetadata : (List<AttributeMetadata>) collectedIdAttributes ) {
			if ( attributeName.equals( attributeMetadata.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private List<String> idClassMemberNames(ClassDetails idClassDetails) {
		final ArrayList<String> memberNames = new ArrayList<>();
		ClassDetails currentType = idClassDetails;
		while ( currentType != null && currentType != ClassDetails.OBJECT_CLASS_DETAILS ) {
			for ( MemberDetails field : currentType.getFields() ) {
				if ( !field.isPersistable() ) {
					continue;
				}
				final String attributeName = field.resolveAttributeName();
				if ( attributeName != null && !memberNames.contains( attributeName ) ) {
					memberNames.add( attributeName );
				}
			}
			for ( MemberDetails method : currentType.getMethods() ) {
				if ( !method.isPersistable() ) {
					continue;
				}
				final String attributeName = method.resolveAttributeName();
				if ( attributeName != null && !memberNames.contains( attributeName ) ) {
					memberNames.add( attributeName );
				}
			}
			currentType = currentType.getSuperClass();
		}
		return memberNames;
	}

	public KeyMapping getNaturalIdMapping() {
		if ( collectedNaturalIdAttributes == null ) {
			return null;
		}

		if ( collectedNaturalIdAttributes instanceof List ) {
			//noinspection unchecked
			final List<AttributeMetadata> attributes = (List<AttributeMetadata>) collectedNaturalIdAttributes;
			return new NonAggregatedKeyMappingImpl( attributes, null );
		}

		final AttributeMetadata attribute = (AttributeMetadata) collectedNaturalIdAttributes;

		if ( attribute.getNature() == AttributeNature.BASIC ) {
			return new BasicKeyMappingImpl( attribute );
		}

		if ( attribute.getNature() == AttributeNature.EMBEDDED ) {
			return new AggregatedKeyMappingImpl( attribute );
		}

		if ( attribute.getNature() == AttributeNature.TO_ONE ) {
			return new NonAggregatedKeyMappingImpl( List.of( attribute ), null );
		}

		throw new ModelsException(
				String.format(
						Locale.ROOT,
						"Unexpected attribute nature [%s] - %s",
						attribute.getNature(),
						entityHierarchy.getRoot().getEntityName()
				)
		);
	}

	public void collectType(IdentifiableTypeMetadata typeMetadata) {
		if ( typeMetadata instanceof MappedSuperclassTypeMetadata mappedSuperclass ) {
			mappedSuperclassTracker.markVisited( mappedSuperclass );
		}

		if ( belowRootEntity ) {
			if ( hasNaturalIdAttribute( typeMetadata ) ) {
				throw new AnnotationException(
						String.format(
								Locale.ROOT,
								"Natural-id attributes must be declared by the root entity [%s] or its mapped superclasses",
								rootEntityClassDetails.getName()
						)
				);
			}
			if ( typeMetadata instanceof EntityTypeMetadata && hasNewIdentifierAttribute( typeMetadata ) ) {
				throw new AnnotationException(
						String.format(
								Locale.ROOT,
								"Entity subclass `%s` may not declare identifier attributes",
								typeMetadata.getClassDetails().getName()
						)
				);
			}
			return;
		}

		final ClassDetails classDetails = typeMetadata.getClassDetails();

		if ( sameClass( classDetails, rootEntityClassDetails ) ) {
			rootEntityMetadata = (EntityTypeMetadata) typeMetadata;
			belowRootEntity = true;
		}

		inheritanceAnnotation = applyLocalAnnotation( Inheritance.class, classDetails, inheritanceAnnotation );
		optimisticLockingAnnotation = applyLocalAnnotation( OptimisticLocking.class, classDetails, optimisticLockingAnnotation );
		cacheAnnotation = applyLocalAnnotation( Cache.class, classDetails, cacheAnnotation );
		naturalIdCacheAnnotation = applyLocalAnnotation( NaturalIdCache.class, classDetails, naturalIdCacheAnnotation );
		idClassAnnotation = applyLocalAnnotation( IdClass.class, classDetails, idClassAnnotation );

		if ( versionAttribute == null || tenantIdAttribute == null || !( collectedIdAttributes instanceof AttributeMetadata idAttribute
				&& idAttribute.getMember().hasDirectAnnotationUsage( EmbeddedId.class ) ) ) {
			// walk the attributes
			typeMetadata.forEachAttribute( (index, attributeMetadata) -> {
				final MemberDetails attributeMember = attributeMetadata.getMember();

				if ( attributeMember.hasDirectAnnotationUsage( EmbeddedId.class )
						&& ( collectedIdAttributes == null || idClassAnnotation != null ) ) {
					collectIdAttribute( attributeMetadata );
				}

				if ( attributeMember.hasDirectAnnotationUsage( Id.class ) ) {
					collectIdAttribute( attributeMetadata );
				}

				if ( attributeMember.hasDirectAnnotationUsage( NaturalId.class ) ) {
					collectNaturalIdAttribute( attributeMetadata );
				}

				if ( versionAttribute == null ) {
					if ( attributeMember.hasDirectAnnotationUsage( Version.class ) ) {
						versionAttribute = attributeMetadata;
					}
				}

				if ( tenantIdAttribute == null ) {
					if ( attributeMember.hasDirectAnnotationUsage( TenantId.class ) ) {
						tenantIdAttribute = attributeMetadata;
					}
				}
			} );
		}
	}

	private boolean hasNaturalIdAttribute(IdentifiableTypeMetadata typeMetadata) {
		final boolean[] found = { false };
		typeMetadata.forEachAttribute( (index, attributeMetadata) -> {
			if ( attributeMetadata.getMember().hasDirectAnnotationUsage( NaturalId.class ) ) {
				found[0] = true;
			}
		} );
		return found[0];
	}

	private boolean hasNewIdentifierAttribute(IdentifiableTypeMetadata typeMetadata) {
		final boolean[] found = { false };
		typeMetadata.forEachAttribute( (index, attributeMetadata) -> {
			final MemberDetails member = attributeMetadata.getMember();
			if ( member.hasDirectAnnotationUsage( Id.class )
					|| member.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
				found[0] = found[0] || !hasCollectedIdAttribute( attributeMetadata.getName() );
			}
		} );
		return found[0];
	}

	public boolean shouldProcessSubType(ClassDetails baseClassDetails, ClassDetails subClassDetails) {
		if ( sameClass( baseClassDetails, rootEntityClassDetails )
				|| hasSuperType( baseClassDetails, rootEntityClassDetails ) ) {
			return true;
		}

		return sameClass( subClassDetails, rootEntityClassDetails )
				|| hasSuperType( rootEntityClassDetails, subClassDetails );
	}

	private boolean hasSuperType(ClassDetails classDetails, ClassDetails possibleSuperType) {
		ClassDetails current = classDetails.getSuperClass();
		while ( current != null ) {
			if ( sameClass( current, possibleSuperType ) ) {
				return true;
			}
			current = current.getSuperClass();
		}
		return false;
	}

	private static boolean sameClass(ClassDetails one, ClassDetails another) {
		if ( one == another ) {
			return true;
		}

		final String oneClassName = one.getClassName();
		return oneClassName != null && oneClassName.equals( another.getClassName() );
	}

	private <A extends Annotation> A applyLocalAnnotation(Class<A> annotationType, ClassDetails classDetails, A currentValue) {
		final A localAnnotation = classDetails.getDirectAnnotationUsage( annotationType );
		if ( localAnnotation != null ) {
			if ( currentValue != null ) {
				CATEGORIZATION_LOGGER.debugf(
						"Ignoring @%s from %s in favor of usage from %s",
						annotationType.getSimpleName(),
						classDetails.getName(),
						currentValue.annotationType().getName()
				);
			}

			// the one "closest" to the root-entity should win
			return localAnnotation;
		}

		return currentValue;
	}

	private ClassDetails classDetails(Class<?> javaClass) {
		return modelContext
				.getClassDetailsRegistry()
				.resolveClassDetails( javaClass.getName() );
	}

	private ClassDetails resolveGeneratedIdClassDetails() {
		try {
			final String generatedIdClassName = generatedClassName( rootEntityClassDetails.toJavaClass() ) + "$Id";
			return modelContext.getClassDetailsRegistry().resolveClassDetails( generatedIdClassName );
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	private static String generatedClassName(Class<?> javaClass) {
		return javaClass.isMemberClass()
				? generatedClassName( javaClass.getEnclosingClass() ) + "$" + javaClass.getSimpleName() + "_"
				: javaClass.getName() + "_";
	}

	public void collectIdAttribute(AttributeMetadata member) {
		assert member != null;

		if ( member.getMember().hasDirectAnnotationUsage( Formula.class ) ) {
			throw new CannotForceNonNullableException(
					"Identifier property '" + member.getName() + "' cannot map to a '@Formula'"
			);
		}

		if ( collectedIdAttributes == null ) {
			collectedIdAttributes = member;
		}
		else if ( collectedIdAttributes instanceof List ) {
			//noinspection unchecked,rawtypes
			final List<AttributeMetadata> membersList = (List) collectedIdAttributes;
			membersList.add( member );
		}
		else if ( collectedIdAttributes instanceof AttributeMetadata ) {
			final ArrayList<AttributeMetadata> combined = new ArrayList<>();
			combined.add( (AttributeMetadata) collectedIdAttributes );
			combined.add( member );
			collectedIdAttributes = combined;
		}
	}

	public void collectNaturalIdAttribute(AttributeMetadata member) {
		assert member != null;

		if ( collectedNaturalIdAttributes == null ) {
			collectedNaturalIdAttributes = member;
		}
		else if ( collectedNaturalIdAttributes instanceof List ) {
			//noinspection unchecked,rawtypes
			final List<AttributeMetadata> membersList = (List) collectedNaturalIdAttributes;
			membersList.add( member );
		}
		else if ( collectedNaturalIdAttributes instanceof AttributeMetadata ) {
			final ArrayList<AttributeMetadata> combined = new ArrayList<>();
			combined.add( (AttributeMetadata) collectedNaturalIdAttributes );
			combined.add( member );
			collectedNaturalIdAttributes = combined;
		}
	}
}
