/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.Locale;

import jakarta.annotation.Nonnull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.boot.mapping.spi.EntityHierarchy;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/// Internal entity hierarchy.
///
/// @since 9.0
/// @author Steve Ebersole
public class EntityHierarchyImpl implements EntityHierarchy {
	private final AbstractIdentifiableTypeMetadata absoluteRootTypeMetadata;
	private final EntityTypeMetadataImpl rootEntityTypeMetadata;

	private final InheritanceType inheritanceType;
	private final AccessType defaultAccessType;
	private final OptimisticLockStyle optimisticLockStyle;

	private final KeyMapping idMapping;
	private final IdentifierGeneratorResolution identifierGeneratorResolution;
	private final KeyMapping naturalIdMapping;
	private final AttributeMetadataImplementor versionAttribute;
	private final AttributeMetadataImplementor tenantIdAttribute;

	private final CacheRegion cacheRegion;
	private final NaturalIdCacheRegion naturalIdCacheRegion;

	public EntityHierarchyImpl(
			ClassDetails rootEntityClassDetails,
			AccessType defaultAccessType,
			org.hibernate.cache.spi.access.AccessType defaultCacheAccessType,
			ManagedTypeInheritanceState inheritanceState,
			MappedSuperclassTracker mappedSuperclassTracker,
			CategorizationContext modelBuildingContext) {
		this.defaultAccessType = defaultAccessType;

		final ClassDetails absoluteRootClassDetails = findRootRoot( rootEntityClassDetails, inheritanceState );
		final HierarchyMetadataCollector metadataCollector = new HierarchyMetadataCollector(
				this,
				rootEntityClassDetails,
				modelBuildingContext,
				mappedSuperclassTracker
		);

		if ( CategorizationHelper.isEntity( absoluteRootClassDetails ) ) {
			this.absoluteRootTypeMetadata = new EntityTypeMetadataImpl(
					absoluteRootClassDetails,
					this,
					inheritanceState,
					metadataCollector,
					modelBuildingContext
			);
		}
		else {
			assert CategorizationHelper.isMappedSuperclass( absoluteRootClassDetails );
			this.absoluteRootTypeMetadata = new MappedSuperclassTypeMetadataImpl(
					absoluteRootClassDetails,
					this,
					inheritanceState,
					metadataCollector,
					modelBuildingContext
			);
		}

		this.rootEntityTypeMetadata = metadataCollector.getRootEntityMetadata();
		assert rootEntityTypeMetadata != null;

		this.inheritanceType = determineInheritanceType( metadataCollector );
		this.optimisticLockStyle = determineOptimisticLockStyle( metadataCollector );

		this.idMapping = metadataCollector.getIdMapping();
		this.identifierGeneratorResolution = IdentifierGeneratorResolutionResolver.resolve(
				this,
				idMapping,
				modelBuildingContext
		);
		this.naturalIdMapping = metadataCollector.getNaturalIdMapping();
		this.versionAttribute = metadataCollector.getVersionAttribute();
		this.tenantIdAttribute = metadataCollector.getTenantIdAttribute();

		this.cacheRegion = determineCacheRegion( metadataCollector, defaultCacheAccessType );
		this.naturalIdCacheRegion = determineNaturalIdCacheRegion( metadataCollector, cacheRegion );
	}

	private ClassDetails findRootRoot(
			ClassDetails rootEntityClassDetails,
			ManagedTypeInheritanceState inheritanceState) {
		if ( inheritanceState == null ) {
			return findRootRoot( rootEntityClassDetails );
		}

		ClassDetails result = rootEntityClassDetails;
		ClassDetails superType = inheritanceState.getSuperType( result );
		while ( superType != null ) {
			result = superType;
			superType = inheritanceState.getSuperType( result );
		}

		return result;
	}

	private ClassDetails findRootRoot(ClassDetails rootEntityClassDetails) {
		ClassDetails result = rootEntityClassDetails;
		ClassDetails current = rootEntityClassDetails.getSuperClass();
		while ( current != null ) {
			if ( CategorizationHelper.isIdentifiable( current ) ) {
				result = current;
			}
			current = current.getSuperClass();
		}
		return result;
	}

	@Override @Nonnull
	public EntityTypeMetadataImpl getRoot() {
		return rootEntityTypeMetadata;
	}

	@Override @Nonnull
	public AbstractIdentifiableTypeMetadata getAbsoluteRoot() {
		return absoluteRootTypeMetadata;
	}

	@Override @Nonnull
	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	@Override @Nonnull
	public AccessType getDefaultAccessType() {
		return defaultAccessType;
	}

	@Nonnull
	public KeyMapping getIdMapping() {
		return idMapping;
	}

	@Nonnull
	public IdentifierGeneratorResolution getIdentifierGeneratorResolution() {
		return identifierGeneratorResolution;
	}

	public KeyMapping getNaturalIdMapping() {
		return naturalIdMapping;
	}

	public AttributeMetadataImplementor getVersionAttribute() {
		return versionAttribute;
	}

	public AttributeMetadataImplementor getTenantIdAttribute() {
		return tenantIdAttribute;
	}

	@Nonnull
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Nonnull
	public CacheRegion getCacheRegion() {
		return cacheRegion;
	}

	@Nonnull
	public NaturalIdCacheRegion getNaturalIdCacheRegion() {
		return naturalIdCacheRegion;
	}

	public void forEachType(HierarchyTypeVisitor typeVisitor) {
		final AbstractIdentifiableTypeMetadata absoluteRoot = getAbsoluteRoot();
		final HierarchyRelation hierarchyRelation;
		if ( absoluteRoot == getRoot() ) {
			hierarchyRelation = HierarchyRelation.ROOT;
		}
		else {
			hierarchyRelation = HierarchyRelation.SUPER;
		}

		forEachType( absoluteRoot, null, hierarchyRelation, typeVisitor );
	}

	private void forEachType(
			AbstractIdentifiableTypeMetadata type,
			AbstractIdentifiableTypeMetadata superType,
			HierarchyRelation hierarchyRelation,
			HierarchyTypeVisitor typeVisitor) {
		typeVisitor.visitType( type, superType, this, hierarchyRelation );

		type.forEachSubType( subType -> {
			final HierarchyRelation nextRelation;
			if ( sameClass( subType.getClassDetails(), getRoot().getClassDetails() ) ) {
				nextRelation = HierarchyRelation.ROOT;
			}
			else if ( hierarchyRelation == HierarchyRelation.SUPER ) {
				nextRelation = HierarchyRelation.SUPER;
			}
			else {
				nextRelation = HierarchyRelation.SUB;
			}
			forEachType( subType, type, nextRelation, typeVisitor );
		} );
	}

	private static boolean sameClass(ClassDetails one, ClassDetails another) {
		if ( one == another ) {
			return true;
		}

		final String oneClassName = one.getClassName();
		return oneClassName != null && oneClassName.equals( another.getClassName() );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"EntityHierarchyImpl(`%s` (%s))",
				rootEntityTypeMetadata.getEntityName(),
				inheritanceType.name()
		);
	}

	/// Describes a type's place in the hierarchy relative to the root entity.
	public enum HierarchyRelation { SUPER, ROOT, SUB }

	@FunctionalInterface
	public interface HierarchyTypeVisitor {
		void visitType(
				AbstractIdentifiableTypeMetadata type,
				AbstractIdentifiableTypeMetadata superType,
				EntityHierarchyImpl hierarchy,
				HierarchyRelation relation);
	}


	private static final OptimisticLockStyle DEFAULT_LOCKING_STRATEGY = OptimisticLockStyle.VERSION;

	private InheritanceType determineInheritanceType(HierarchyMetadataCollector metadataCollector) {
		if ( CategorizationLogging.CATEGORIZATION_LOGGER.isDebugEnabled() ) {
			// Validate that there is no @Inheritance annotation further down the hierarchy
			ensureNoInheritanceAnnotationsOnSubclasses( rootEntityTypeMetadata );
		}

		final Inheritance inheritanceAnnotation = metadataCollector.getInheritanceAnnotation();
		if ( inheritanceAnnotation != null ) {
			return inheritanceAnnotation.strategy();
		}

		return InheritanceType.SINGLE_TABLE;
	}

	private OptimisticLockStyle determineOptimisticLockStyle(HierarchyMetadataCollector metadataCollector) {
		final OptimisticLocking optimisticLockingAnnotation = metadataCollector.getOptimisticLockingAnnotation();
		if ( optimisticLockingAnnotation != null ) {
			return OptimisticLockStyle.valueOf( optimisticLockingAnnotation.type().name() );
		}
		return DEFAULT_LOCKING_STRATEGY;
	}

	private CacheRegion determineCacheRegion(
			HierarchyMetadataCollector metadataCollector,
			org.hibernate.cache.spi.access.AccessType defaultCacheAccessType) {
		final Cache cacheAnnotation = metadataCollector.getCacheAnnotation();
		return new CacheRegion( cacheAnnotation, defaultCacheAccessType, rootEntityTypeMetadata.getEntityName() );
	}

	private NaturalIdCacheRegion determineNaturalIdCacheRegion(
			HierarchyMetadataCollector metadataCollector,
			CacheRegion cacheRegion) {
		final NaturalIdCache naturalIdCacheAnnotation = metadataCollector.getNaturalIdCacheAnnotation();
		if ( naturalIdCacheAnnotation == null ) {
			return null;
		}
		return new NaturalIdCacheRegion( naturalIdCacheAnnotation, cacheRegion );
	}

	/**
	 * Find the InheritanceType from the locally defined {@link Inheritance} annotation,
	 * if one.  Returns {@code null} if {@link Inheritance} is not locally defined.
	 *
	 * @apiNote Used when building the {@link EntityHierarchyImpl}
	 */
	private static InheritanceType getLocallyDefinedInheritanceType(ClassDetails managedClass) {
		final Inheritance localAnnotation = managedClass.getDirectAnnotationUsage( Inheritance.class );
		if ( localAnnotation == null ) {
			return null;
		}

		return localAnnotation.strategy();
	}

	private void ensureNoInheritanceAnnotationsOnSubclasses(AbstractIdentifiableTypeMetadata type) {
		type.forEachSubType( (subType) -> {
			if ( getLocallyDefinedInheritanceType( subType.getClassDetails() ) != null ) {
				CategorizationLogging.CATEGORIZATION_LOGGER.debugf(
						"@javax.persistence.Inheritance was specified on non-root entity [%s]; ignoring...",
						type.getClassDetails().getName()
				);
			}
			ensureNoInheritanceAnnotationsOnSubclasses( subType );
		} );
	}

}
