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
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.EntityMode;
import org.hibernate.TruthValue;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.IdType;
import org.hibernate.metamodel.source.internal.annotations.entity.RootEntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.DiscriminatorSource;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.MultiTenancySource;
import org.hibernate.metamodel.source.spi.VersionAttributeSource;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.InheritanceType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

/**
 * Adapt the built ManagedTypeMetadata hierarchy to the "source" hierarchy
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class EntityHierarchySourceImpl implements EntityHierarchySource {
	private final InheritanceType inheritanceType;
	private final RootEntitySourceImpl rootEntitySource;

	private final IdentifierSource identifierSource;
	private final OptimisticLockStyle optimisticLockStyle;
	private final VersionAttributeSource versionAttributeSource;
	private final DiscriminatorSource discriminatorSource;

	private final Caching caching;
	private final Caching naturalIdCaching;

	private final MultiTenancySource multiTenancySource;

	private final String whereClause;
	private final String rowId;
	private final boolean mutable;
	private final boolean useExplicitPolymorphism;


	public EntityHierarchySourceImpl(RootEntityTypeMetadata root, InheritanceType inheritanceType) {
		this.inheritanceType = inheritanceType;

		// this starts the "choreographed" creation of the Entity and MappedSuperclass
		// objects making up the hierarchy.  See the discussion on
		// the RootEntitySourceImpl ctor for details...
		this.rootEntitySource = new RootEntitySourceImpl( root, this );

		this.identifierSource = determineIdentifierSource( root, rootEntitySource );

		this.optimisticLockStyle = determineOptimisticLockStyle( root );
		this.versionAttributeSource = determineVersionAttributeSource( root );
		this.discriminatorSource = determineDiscriminatorSource( root );

		this.caching = determineCachingSettings( root );
		this.naturalIdCaching = determineNaturalIdCachingSettings( root );

		this.multiTenancySource = determineMultiTenancySource( root );

		// (im)mutability
		final AnnotationInstance hibernateImmutableAnnotation = root.getJavaTypeDescriptor()
				.findTypeAnnotation( HibernateDotNames.IMMUTABLE );
		this.mutable = ( hibernateImmutableAnnotation == null );

		// implicit/explicit polymorphism (see HHH-6400)
		PolymorphismType polymorphism = PolymorphismType.IMPLICIT;
		final AnnotationInstance polymorphismAnnotation = root.getJavaTypeDescriptor()
				.findTypeAnnotation( HibernateDotNames.POLYMORPHISM );
		if ( polymorphismAnnotation != null && polymorphismAnnotation.value( "type" ) != null ) {
			polymorphism = PolymorphismType.valueOf( polymorphismAnnotation.value( "type" ).asEnum() );
		}
		this.useExplicitPolymorphism =  ( polymorphism == PolymorphismType.EXPLICIT );

		// where restriction
		final AnnotationInstance whereAnnotation = root.getJavaTypeDescriptor()
				.findTypeAnnotation( HibernateDotNames.WHERE );
		this.whereClause = whereAnnotation != null && whereAnnotation.value( "clause" ) != null
				? whereAnnotation.value( "clause" ).asString()
				: null;

		this.rowId = root.getRowId();
	}

	private IdentifierSource determineIdentifierSource(RootEntityTypeMetadata root, RootEntitySourceImpl rootSource) {
		final IdType idType = root.getIdType();

		switch ( idType ) {
			case SIMPLE: {
				return new SimpleIdentifierSourceImpl(
						rootSource,
						(SingularAttributeSourceImpl) rootSource.getIdentifierAttributes().get( 0 )
				);
			}
			case AGGREGATED: {
				return new AggregatedCompositeIdentifierSourceImpl(
						rootSource,
						(EmbeddedAttributeSourceImpl) rootSource.getIdentifierAttributes().get( 0 ),
						rootSource.getMapsIdSources()
				);
			}
			case NON_AGGREGATED: {
				return new NonAggregatedCompositeIdentifierSourceImpl( rootSource );
			}
			default: {
				throw root.getLocalBindingContext().makeMappingException(
						"Entity did not define an identifier"
				);
			}
		}
	}

	private Caching determineCachingSettings(EntityTypeMetadata root) {
		// I am not so sure that we should be interpreting SharedCacheMode here.
		// Caching accepts a TruthValue value for this purpose.  Might be better
		// to unify this in Binder or in SessionFactoryImpl

		Caching caching = new Caching( TruthValue.UNKNOWN );

		final AnnotationInstance hibernateCacheAnnotation = root.getJavaTypeDescriptor().findTypeAnnotation( HibernateDotNames.CACHE );
		if ( hibernateCacheAnnotation != null ) {
			applyRequestedHibernateCachingValues( caching, hibernateCacheAnnotation );
			return caching;
		}

		applyJpaCachingValues(
				root.getLocalBindingContext(),
				caching,
				root.getJavaTypeDescriptor().findTypeAnnotation( JPADotNames.CACHEABLE )
		);

		return caching;
	}

	private void applyRequestedHibernateCachingValues(Caching caching, AnnotationInstance hibernateCacheAnnotation) {
		caching.setRequested( TruthValue.TRUE );

		if ( hibernateCacheAnnotation.value( "usage" ) != null ) {
			caching.setAccessType(
					CacheConcurrencyStrategy.parse( hibernateCacheAnnotation.value( "usage" ).asEnum() ).toAccessType()
			);
		}

		if ( hibernateCacheAnnotation.value( "region" ) != null ) {
			caching.setRegion( hibernateCacheAnnotation.value( "region" ).asString() );
		}

		caching.setCacheLazyProperties(
				hibernateCacheAnnotation.value( "include" ) != null
						&& "all".equals( hibernateCacheAnnotation.value( "include" ).asString() )
		);
	}

	private void applyJpaCachingValues(
			EntityBindingContext localBindingContext,
			Caching caching,
			AnnotationInstance jpaCacheableAnnotation) {
		// todo : note a fan of applying SharedCacheMode here.
		//		imo this should be handled by Binder
		switch ( localBindingContext.getBuildingOptions().getSharedCacheMode() ) {
			case ALL: {
				caching.setRequested( TruthValue.TRUE );
				break;
			}
			case ENABLE_SELECTIVE: {
				// In the ENABLE_SELECTIVE case, the @Cacheable annotation must be present
				//	and its value must be true
				if ( jpaCacheableAnnotation == null ) {
					// No annotation present, so we do not enable caching
					caching.setRequested( TruthValue.FALSE );
				}
				else {
					boolean value = JandexHelper.getValue(
							jpaCacheableAnnotation,
							"value",
							Boolean.class,
							localBindingContext.getServiceRegistry().getService( ClassLoaderService.class )
					);
					// we enable caching if the value was true
					caching.setRequested( value ? TruthValue.TRUE : TruthValue.FALSE );
				}
				break;
			}
			case DISABLE_SELECTIVE: {
				// In the DISABLE_SELECTIVE case we enable caching for all entities
				// unless it explicitly says to not too
				if ( jpaCacheableAnnotation == null ) {
					// No annotation present, so the entity did not explicitly opt out
					// of caching
					caching.setRequested( TruthValue.TRUE );
				}
				else {
					boolean value = JandexHelper.getValue(
							jpaCacheableAnnotation,
							"value",
							Boolean.class,
							localBindingContext.getServiceRegistry().getService( ClassLoaderService.class )
					);
					// we enable caching if the value was true
					caching.setRequested( value ? TruthValue.TRUE : TruthValue.FALSE );
				}
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				caching.setRequested( TruthValue.FALSE );
				break;
			}
		}

		if ( caching.getRequested() != TruthValue.FALSE ) {
			caching.setCacheLazyProperties( true );
		}
	}

	private Caching determineNaturalIdCachingSettings(EntityTypeMetadata root) {
		Caching naturalIdCaching = new Caching( TruthValue.FALSE );

		final AnnotationInstance naturalIdCacheAnnotation = root.getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.NATURAL_ID_CACHE
		);
		if ( naturalIdCacheAnnotation != null ) {
			if ( naturalIdCacheAnnotation.value( "region" ) != null ) {
				String region = naturalIdCacheAnnotation.value( "region" ).asString();
				if ( StringHelper.isNotEmpty( region ) ) {
					naturalIdCaching.setRegion( region );
				}
			}
			naturalIdCaching.setRequested( TruthValue.TRUE );
		}

		return naturalIdCaching;
	}

	private OptimisticLockStyle determineOptimisticLockStyle(EntityTypeMetadata root) {
		OptimisticLockStyle style = OptimisticLockStyle.VERSION;
		final AnnotationInstance optimisticLocking = JandexHelper.getSingleAnnotation(
				root.getJavaTypeDescriptor().getJandexClassInfo(),
				HibernateDotNames.OPTIMISTIC_LOCKING,
				ClassInfo.class
		);
		if ( optimisticLocking != null && optimisticLocking.value( "type" ) != null ) {
			style = OptimisticLockStyle.valueOf( optimisticLocking.value( "type" ).asEnum() );
		}
		return style;
	}

	private VersionAttributeSource determineVersionAttributeSource(RootEntityTypeMetadata root) {
		if ( root.getVersionAttribute() == null ) {
			return null;
		}
		return new VersionAttributeSourceImpl( root.getVersionAttribute(), root );
	}

	private DiscriminatorSource determineDiscriminatorSource(EntityTypeMetadata root) {
		switch ( inheritanceType ) {
			case JOINED: {
				if ( root.containsDiscriminator() ) {
					return root.getLocalBindingContext().getBuildingOptions().ignoreExplicitDiscriminatorsForJoinedInheritance()
							? null
							: new DiscriminatorSourceImpl( root );
				}
				else {
					return root.getLocalBindingContext().getBuildingOptions().createImplicitDiscriminatorsForJoinedInheritance()
							? new ImplicitDiscriminatorSourceImpl( root )
							: null;
				}
			}
			case SINGLE_TABLE: {
				return root.containsDiscriminator()
						? new DiscriminatorSourceImpl( root )
						: new ImplicitDiscriminatorSourceImpl( root );
			}
			case TABLE_PER_CLASS: {
				return null;
			}
			case NO_INHERITANCE: {
				return null;
			}
			default: {
				return null;
			}
		}
	}

	private MultiTenancySource determineMultiTenancySource(EntityTypeMetadata root) {
		return root.hasMultiTenancySourceInformation()
				? new MutliTenancySourceImpl( root )
				: null;
	}

	@Override
	public EntitySource getRoot() {
		return rootEntitySource;
	}

	@Override
	public InheritanceType getHierarchyInheritanceType() {
		return inheritanceType;
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		return identifierSource;
	}

	@Override
	public VersionAttributeSource getVersionAttributeSource() {
		return versionAttributeSource;
	}

	@Override
	public DiscriminatorSource getDiscriminatorSource() {
		return discriminatorSource;
	}

	@Override
	public MultiTenancySource getMultiTenancySource() {
		return multiTenancySource;
	}

	@Override
	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return useExplicitPolymorphism;
	}

	@Override
	public String getWhere() {
		return whereClause;
	}

	@Override
	public String getRowId() {
		return rowId;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public Caching getNaturalIdCaching() {
		return naturalIdCaching;
	}

	@Override
	public String toString() {
		return "EntityHierarchySourceImpl{rootEntitySource=" + rootEntitySource.getEntityName()
				+ ", inheritanceType=" + inheritanceType + '}';
	}
}


