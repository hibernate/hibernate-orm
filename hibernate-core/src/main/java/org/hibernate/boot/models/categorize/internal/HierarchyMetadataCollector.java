/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.ModelsException;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizationContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Version;

import static org.hibernate.boot.models.categorize.CategorizationLogging.CATEGORIZATION_LOGGER;

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
		if ( collectedIdAttributes instanceof List ) {
			//noinspection unchecked
			final List<AttributeMetadata> idAttributes = (List<AttributeMetadata>) collectedIdAttributes;
			final ClassDetails idClassDetails;
			if ( idClassAnnotation == null ) {
				idClassDetails = null;
			}
			else {
				idClassDetails = classDetails( idClassAnnotation.value() );
			}
			return new NonAggregatedKeyMappingImpl( idAttributes, idClassDetails );
		}

		final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;
		if ( idAttribute == null ) {
			throw new MappingException(
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

	public void collectIdAttribute(AttributeMetadata member) {
		assert member != null;

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
