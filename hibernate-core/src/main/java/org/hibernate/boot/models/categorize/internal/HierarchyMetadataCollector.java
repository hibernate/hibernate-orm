/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Version;

import static org.hibernate.boot.models.categorize.ModelCategorizationLogging.MODEL_CATEGORIZATION_LOGGER;

/**
 * Used to collect useful details about a hierarchy as we build its metadata
 *
 * @implNote The HierarchyTypeConsumer is called from root down.  We make use
 * of that detail in a number of places here in the code.
 *
 * @author Steve Ebersole
 */
public class HierarchyMetadataCollector implements HierarchyTypeConsumer {
	private final EntityHierarchy entityHierarchy;
	private final ClassDetails rootEntityClassDetails;
	private final HierarchyTypeConsumer delegateConsumer;

	private boolean belowRootEntity;

	private EntityTypeMetadata rootEntityMetadata;
	private AnnotationUsage<Inheritance> inheritanceAnnotation;
	private AnnotationUsage<OptimisticLocking> optimisticLockingAnnotation;
	private AnnotationUsage<Cache> cacheAnnotation;
	private AnnotationUsage<NaturalIdCache> naturalIdCacheAnnotation;

	private KeyMapping idMapping;
	private AttributeMetadata versionAttribute;
	private AttributeMetadata tenantIdAttribute;

	private AnnotationUsage<IdClass> idClassAnnotation;
	private Object collectedIdAttributes;
	private Object collectedNaturalIdAttributes;

	public HierarchyMetadataCollector(
			EntityHierarchy entityHierarchy,
			ClassDetails rootEntityClassDetails,
			HierarchyTypeConsumer delegateConsumer) {
		this.entityHierarchy = entityHierarchy;
		this.rootEntityClassDetails = rootEntityClassDetails;
		this.delegateConsumer = delegateConsumer;
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

	public AnnotationUsage<Inheritance> getInheritanceAnnotation() {
		return inheritanceAnnotation;
	}

	public AttributeMetadata getVersionAttribute() {
		return versionAttribute;
	}

	public AttributeMetadata getTenantIdAttribute() {
		return tenantIdAttribute;
	}

	public AnnotationUsage<OptimisticLocking> getOptimisticLockingAnnotation() {
		return optimisticLockingAnnotation;
	}

	public AnnotationUsage<Cache> getCacheAnnotation() {
		return cacheAnnotation;
	}

	public AnnotationUsage<NaturalIdCache> getNaturalIdCacheAnnotation() {
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
				idClassDetails = idClassAnnotation.getAttributeValue( "value" );
			}
			return new NonAggregatedKeyMappingImpl( idAttributes, idClassDetails );
		}

		final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;

		if ( idAttribute.getNature() == AttributeMetadata.AttributeNature.BASIC ) {
			return new BasicKeyMappingImpl( idAttribute );
		}

		if ( idAttribute.getNature() == AttributeMetadata.AttributeNature.EMBEDDED ) {
			return new AggregatedKeyMappingImpl( idAttribute );
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

		if ( attribute.getNature() == AttributeMetadata.AttributeNature.BASIC ) {
			return new BasicKeyMappingImpl( attribute );
		}

		if ( attribute.getNature() == AttributeMetadata.AttributeNature.EMBEDDED ) {
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

	@Override
	public void acceptType(IdentifiableTypeMetadata typeMetadata) {
		if ( delegateConsumer != null ) {
			delegateConsumer.acceptType( typeMetadata );
		}

		if ( belowRootEntity ) {
			return;
		}

		final ClassDetails classDetails = typeMetadata.getClassDetails();

		if ( classDetails == rootEntityClassDetails ) {
			rootEntityMetadata = (EntityTypeMetadata) typeMetadata;
			belowRootEntity = true;
		}

		inheritanceAnnotation = applyLocalAnnotation( Inheritance.class, classDetails, inheritanceAnnotation );
		optimisticLockingAnnotation = applyLocalAnnotation( OptimisticLocking.class, classDetails, optimisticLockingAnnotation );
		cacheAnnotation = applyLocalAnnotation( Cache.class, classDetails, cacheAnnotation );
		naturalIdCacheAnnotation = applyLocalAnnotation( NaturalIdCache.class, classDetails, naturalIdCacheAnnotation );
		idClassAnnotation = applyLocalAnnotation( IdClass.class, classDetails, idClassAnnotation );

		final boolean collectIds = collectedIdAttributes == null;
		if ( collectIds || versionAttribute == null || tenantIdAttribute == null ) {
			// walk the attributes
			typeMetadata.forEachAttribute( (index, attributeMetadata) -> {
				final MemberDetails attributeMember = attributeMetadata.getMember();

				if ( collectIds ) {
					final AnnotationUsage<EmbeddedId> eIdAnn = attributeMember.getAnnotationUsage( EmbeddedId.class );
					if ( eIdAnn != null ) {
						collectIdAttribute( attributeMetadata );
					}

					final AnnotationUsage<Id> idAnn = attributeMember.getAnnotationUsage( Id.class );
					if ( idAnn != null ) {
						collectIdAttribute( attributeMetadata );
					}
				}

				if ( attributeMember.getAnnotationUsage( NaturalId.class ) != null ) {
					collectNaturalIdAttribute( attributeMetadata );
				}

				if ( versionAttribute == null ) {
					if ( attributeMember.getAnnotationUsage( Version.class ) != null ) {
						versionAttribute = attributeMetadata;
					}
				}

				if ( tenantIdAttribute == null ) {
					if ( attributeMember.getAnnotationUsage( TenantId.class ) != null ) {
						tenantIdAttribute = attributeMetadata;
					}
				}
			} );
		}
	}

	private <A extends Annotation> AnnotationUsage<A> applyLocalAnnotation(Class<A> annotationType, ClassDetails classDetails, AnnotationUsage<A> currentValue) {
		final AnnotationUsage<A> localInheritanceAnnotation = classDetails.getAnnotationUsage( annotationType );
		if ( localInheritanceAnnotation != null ) {
			if ( currentValue != null ) {
				MODEL_CATEGORIZATION_LOGGER.debugf(
						"Ignoring @%s from %s in favor of usage from %s",
						annotationType.getSimpleName(),
						classDetails.getName(),
						currentValue.getAnnotationTarget().getName()
				);
			}

			// the one "closest" to the root-entity should win
			return localInheritanceAnnotation;
		}

		return currentValue;
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
