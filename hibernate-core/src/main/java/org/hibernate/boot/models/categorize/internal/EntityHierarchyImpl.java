/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.Locale;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.boot.models.categorize.ModelCategorizationLogging;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CacheRegion;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.boot.models.categorize.spi.NaturalIdCacheRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 *
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private final IdentifiableTypeMetadata absoluteRootTypeMetadata;
	private final EntityTypeMetadata rootEntityTypeMetadata;

	private final InheritanceType inheritanceType;
	private final jakarta.persistence.AccessType defaultAccessType;
	private final OptimisticLockStyle optimisticLockStyle;

	private final KeyMapping idMapping;
	private final KeyMapping naturalIdMapping;
	private final AttributeMetadata versionAttribute;
	private final AttributeMetadata tenantIdAttribute;

	private final CacheRegion cacheRegion;
	private final NaturalIdCacheRegion naturalIdCacheRegion;

	public EntityHierarchyImpl(
			ClassDetails rootEntityClassDetails,
			jakarta.persistence.AccessType defaultAccessType,
			AccessType defaultCacheAccessType,
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext categorizationContext) {
		this.defaultAccessType = defaultAccessType;

		final ClassDetails absoluteRootClassDetails = findRootRoot( rootEntityClassDetails );
		final HierarchyMetadataCollector metadataCollector = new HierarchyMetadataCollector(
				this,
				rootEntityClassDetails,
				typeConsumer,
				categorizationContext
		);

		if ( CategorizationHelper.isEntity( absoluteRootClassDetails ) ) {
			this.rootEntityTypeMetadata = new EntityTypeMetadataImpl(
					absoluteRootClassDetails,
					this,
					defaultAccessType,
					metadataCollector,
					categorizationContext
			);
			this.absoluteRootTypeMetadata = rootEntityTypeMetadata;
		}
		else {
			assert CategorizationHelper.isMappedSuperclass( absoluteRootClassDetails );
			this.absoluteRootTypeMetadata = processRootMappedSuperclasses(
					absoluteRootClassDetails,
					this,
					defaultAccessType,
					metadataCollector,
					categorizationContext
			);
			this.rootEntityTypeMetadata = new EntityTypeMetadataImpl(
					rootEntityClassDetails,
					this,
					(AbstractIdentifiableTypeMetadata) absoluteRootTypeMetadata,
					metadataCollector,
					categorizationContext
			);
		}

		this.inheritanceType = determineInheritanceType( metadataCollector );
		this.optimisticLockStyle = determineOptimisticLockStyle( metadataCollector );

		this.idMapping = metadataCollector.getIdMapping();
		this.naturalIdMapping = metadataCollector.getNaturalIdMapping();
		this.versionAttribute = metadataCollector.getVersionAttribute();
		this.tenantIdAttribute = metadataCollector.getTenantIdAttribute();

		this.cacheRegion = determineCacheRegion( metadataCollector, defaultCacheAccessType );
		this.naturalIdCacheRegion = determineNaturalIdCacheRegion( metadataCollector, cacheRegion );
	}

	private static IdentifiableTypeMetadata processRootMappedSuperclasses(
			ClassDetails absoluteRootClassDetails,
			EntityHierarchyImpl entityHierarchy,
			jakarta.persistence.AccessType defaultAccessType,
			HierarchyMetadataCollector metadataCollector,
			ModelCategorizationContext modelBuildingContext) {
		return new MappedSuperclassTypeMetadataImpl(
				absoluteRootClassDetails,
				entityHierarchy,
				null,
				defaultAccessType,
				metadataCollector,
				modelBuildingContext
		);
	}

	private ClassDetails findRootRoot(ClassDetails rootEntityClassDetails) {
		if ( rootEntityClassDetails.getSuperClass() != null ) {
			final ClassDetails match = walkSupers( rootEntityClassDetails.getSuperClass() );
			if ( match != null ) {
				return match;
			}
		}
		return rootEntityClassDetails;
	}

	private ClassDetails walkSupers(ClassDetails type) {
		assert type != null;

		if ( type.getSuperClass() != null ) {
			final ClassDetails match = walkSupers( type.getSuperClass() );
			if ( match != null ) {
				return match;
			}
		}

		if ( CategorizationHelper.isIdentifiable( type ) ) {
			return type;
		}

		return null;
	}

	@Override
	public EntityTypeMetadata getRoot() {
		return rootEntityTypeMetadata;
	}

	@Override
	public IdentifiableTypeMetadata getAbsoluteRoot() {
		return absoluteRootTypeMetadata;
	}

	@Override
	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	@Override
	public jakarta.persistence.AccessType getDefaultAccessType() {
		return defaultAccessType;
	}

	@Override
	public KeyMapping getIdMapping() {
		return idMapping;
	}

	@Override
	public KeyMapping getNaturalIdMapping() {
		return naturalIdMapping;
	}

	@Override
	public AttributeMetadata getVersionAttribute() {
		return versionAttribute;
	}

	@Override
	public AttributeMetadata getTenantIdAttribute() {
		return tenantIdAttribute;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public CacheRegion getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public NaturalIdCacheRegion getNaturalIdCacheRegion() {
		return naturalIdCacheRegion;
	}

	@Override
	public void forEachType(HierarchyTypeVisitor typeVisitor) {
		final IdentifiableTypeMetadata absoluteRoot = getAbsoluteRoot();
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
			IdentifiableTypeMetadata type,
			IdentifiableTypeMetadata superType,
			HierarchyRelation hierarchyRelation,
			HierarchyTypeVisitor typeVisitor) {
		typeVisitor.visitType( type, superType, this, hierarchyRelation );

		final HierarchyRelation nextRelation;
		if ( hierarchyRelation == HierarchyRelation.SUPER ) {
			if ( type == getRoot().getSuperType() ) {
				// the next iteration will be the root
				nextRelation = HierarchyRelation.ROOT;
			}
			else {
				nextRelation = HierarchyRelation.SUPER;
			}
		}
		else {
			nextRelation = HierarchyRelation.SUB;
		}

		type.forEachSubType( subType -> forEachType( subType, type, nextRelation, typeVisitor ) );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"EntityHierarchy(`%s` (%s))",
				rootEntityTypeMetadata.getEntityName(),
				inheritanceType.name()
		);
	}


	private static final OptimisticLockStyle DEFAULT_LOCKING_STRATEGY = OptimisticLockStyle.VERSION;

	private InheritanceType determineInheritanceType(HierarchyMetadataCollector metadataCollector) {
		if ( ModelCategorizationLogging.MODEL_CATEGORIZATION_LOGGER.isDebugEnabled() ) {
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
			final OptimisticLockType lockingType = optimisticLockingAnnotation.type();
			return OptimisticLockStyle.fromLockType( lockingType );
		}
		return DEFAULT_LOCKING_STRATEGY;
	}

	private CacheRegion determineCacheRegion(
			HierarchyMetadataCollector metadataCollector,
			AccessType defaultCacheAccessType) {
		final Cache cacheAnnotation = metadataCollector.getCacheAnnotation();
		return new CacheRegion( cacheAnnotation, defaultCacheAccessType, rootEntityTypeMetadata.getEntityName() );
	}

	private NaturalIdCacheRegion determineNaturalIdCacheRegion(
			HierarchyMetadataCollector metadataCollector,
			CacheRegion cacheRegion) {
		final NaturalIdCache naturalIdCacheAnnotation = metadataCollector.getNaturalIdCacheAnnotation();
		return new NaturalIdCacheRegion( naturalIdCacheAnnotation, cacheRegion );
	}

	/**
	 * Find the InheritanceType from the locally defined {@link Inheritance} annotation,
	 * if one.  Returns {@code null} if {@link Inheritance} is not locally defined.
	 *
	 * @apiNote Used when building the {@link EntityHierarchy}
	 */
	private static InheritanceType getLocallyDefinedInheritanceType(ClassDetails managedClass) {
		final Inheritance localAnnotation = managedClass.getDirectAnnotationUsage( Inheritance.class );
		if ( localAnnotation == null ) {
			return null;
		}

		return localAnnotation.strategy();
	}

	private void ensureNoInheritanceAnnotationsOnSubclasses(IdentifiableTypeMetadata type) {
		type.forEachSubType( (subType) -> {
			if ( getLocallyDefinedInheritanceType( subType.getClassDetails() ) != null ) {
				ModelCategorizationLogging.MODEL_CATEGORIZATION_LOGGER.debugf(
						"@javax.persistence.Inheritance was specified on non-root entity [%s]; ignoring...",
						type.getClassDetails().getName()
				);
			}
			ensureNoInheritanceAnnotationsOnSubclasses( subType );
		} );
	}

}
