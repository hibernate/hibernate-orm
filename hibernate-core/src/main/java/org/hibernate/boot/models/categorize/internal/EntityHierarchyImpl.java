/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.boot.models.JpaAnnotations;
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
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.Entity;
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
			ModelCategorizationContext modelBuildingContext) {
		assert rootEntityClassDetails.hasAnnotationUsage( Entity.class );

		this.defaultAccessType = defaultAccessType;

		final HierarchyMetadataCollector metadataCollector = new HierarchyMetadataCollector( this, rootEntityClassDetails, typeConsumer );
		final List<ClassDetails> orderedSupers = resolveOrderedSupers( rootEntityClassDetails );

		if ( orderedSupers.isEmpty() ) {
			this.rootEntityTypeMetadata = new EntityTypeMetadataImpl(
					rootEntityClassDetails,
					this,
					metadataCollector,
					modelBuildingContext
			);
			this.absoluteRootTypeMetadata = rootEntityTypeMetadata;
		}
		else {
			final ClassDetails absoluteRootClassDetails = orderedSupers.get( 0 );
			MappedSuperclassTypeMetadataImpl currentSuperTypeMetadata = new MappedSuperclassTypeMetadataImpl(
					absoluteRootClassDetails,
					this,
					null,
					metadataCollector,
					modelBuildingContext
			);
			this.absoluteRootTypeMetadata = currentSuperTypeMetadata;
			if ( orderedSupers.size() > 1 ) {
				for ( int i = 1; i < orderedSupers.size(); i++ ) {
					currentSuperTypeMetadata = new MappedSuperclassTypeMetadataImpl(
							orderedSupers.get(i),
							this,
							currentSuperTypeMetadata,
							metadataCollector,
							modelBuildingContext
					);
				}
			}
			this.rootEntityTypeMetadata = new EntityTypeMetadataImpl(
					rootEntityClassDetails,
					this,
					currentSuperTypeMetadata,
					metadataCollector,
					modelBuildingContext
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

		final AnnotationUsage<Inheritance> inheritanceAnnotation = metadataCollector.getInheritanceAnnotation();
		if ( inheritanceAnnotation != null ) {
			return inheritanceAnnotation.getAttributeValue( "strategy" );
		}

		return InheritanceType.SINGLE_TABLE;
	}

	private OptimisticLockStyle determineOptimisticLockStyle(HierarchyMetadataCollector metadataCollector) {
		final AnnotationUsage<OptimisticLocking> optimisticLockingAnnotation = metadataCollector.getOptimisticLockingAnnotation();
		if ( optimisticLockingAnnotation != null ) {
			optimisticLockingAnnotation.getEnum( "type", DEFAULT_LOCKING_STRATEGY );
		}
		return DEFAULT_LOCKING_STRATEGY;
	}

	private CacheRegion determineCacheRegion(
			HierarchyMetadataCollector metadataCollector,
			AccessType defaultCacheAccessType) {
		final AnnotationUsage<Cache> cacheAnnotation = metadataCollector.getCacheAnnotation();
		return new CacheRegion( cacheAnnotation, defaultCacheAccessType, rootEntityTypeMetadata.getEntityName() );
	}

	private NaturalIdCacheRegion determineNaturalIdCacheRegion(
			HierarchyMetadataCollector metadataCollector,
			CacheRegion cacheRegion) {
		final AnnotationUsage<NaturalIdCache> naturalIdCacheAnnotation = metadataCollector.getNaturalIdCacheAnnotation();
		return new NaturalIdCacheRegion( naturalIdCacheAnnotation, cacheRegion );
	}

	/**
	 * Find the InheritanceType from the locally defined {@link Inheritance} annotation,
	 * if one.  Returns {@code null} if {@link Inheritance} is not locally defined.
	 *
	 * @apiNote Used when building the {@link EntityHierarchy}
	 */
	private static InheritanceType getLocallyDefinedInheritanceType(ClassDetails managedClass) {
		final AnnotationUsage<Inheritance> localAnnotation = managedClass.getAnnotationUsage( JpaAnnotations.INHERITANCE );
		if ( localAnnotation == null ) {
			return null;
		}

		return localAnnotation.getAttributeValue( "strategy" );
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

	private static List<ClassDetails> resolveOrderedSupers(ClassDetails rootEntityClassDetails) {
		final ClassDetails rootEntityDirectSuper = resolveManagedSuper( rootEntityClassDetails );
		if ( rootEntityDirectSuper == null ) {
			return Collections.emptyList();
		}

		final ArrayList<ClassDetails> supers = new ArrayList<>();
		collectSupers( rootEntityDirectSuper, supers );
		return supers;
	}

	private static ClassDetails resolveManagedSuper(ClassDetails managedTypeClassDetails) {
		assert managedTypeClassDetails != null;

		ClassDetails superClassDetails = managedTypeClassDetails.getSuperClass();
		while ( superClassDetails != null
				&& !CategorizationHelper.isIdentifiable( superClassDetails ) ) {
			superClassDetails = superClassDetails.getSuperClass();
		}

		return superClassDetails;
	}

	private static void collectSupers(ClassDetails managedTypeClassDetails, List<ClassDetails> orderedList) {
		final ClassDetails managedSuperClassDetails = resolveManagedSuper( managedTypeClassDetails );
		if ( managedSuperClassDetails != null ) {
			// add supers first
			collectSupers( managedSuperClassDetails, orderedList );
		}

		orderedList.add( managedTypeClassDetails );
	}
}
