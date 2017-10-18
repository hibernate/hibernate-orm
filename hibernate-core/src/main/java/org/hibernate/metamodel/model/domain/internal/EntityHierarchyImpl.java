/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.InheritanceStrategy;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private static final Logger log = Logger.getLogger( EntityHierarchyImpl.class );

	private final EntityDescriptor<?> rootEntityDescriptor;

	private final InheritanceStrategy inheritanceStrategy;
	private final OptimisticLockStyle optimisticLockStyle;
	private final RepresentationMode representationMode;

	private final EntityIdentifier identifierDescriptor;
	private final DiscriminatorDescriptor discriminatorDescriptor;
	private final VersionDescriptor versionDescriptor;
	private final NaturalIdDescriptor naturalIdentifierDescriptor;
	private final RowIdDescriptor rowIdDescriptor;
	private final TenantDiscrimination tenantDiscrimination;

	private final String whereFragment;
	private final boolean mutable;
	private final boolean implicitPolymorphismEnabled;

	private EntityDataAccess caching;

	public EntityHierarchyImpl(
			RuntimeModelCreationContext creationContext,
			EntityDescriptor rootEntityDescriptor,
			RootClass rootEntityBinding) {
		log.debugf( "Creating EntityHierarchy root EntityPersister : %s", rootEntityDescriptor );


		this.rootEntityDescriptor = rootEntityDescriptor;

		this.inheritanceStrategy = interpretInheritanceType( rootEntityBinding );
		this.optimisticLockStyle = rootEntityBinding.getEntityMappingHierarchy().getOptimisticLockStyle();
		this.representationMode = determineRepresentationMode( rootEntityBinding, rootEntityDescriptor, creationContext );

		this.identifierDescriptor = interpretIdentifierDescriptor( this, rootEntityBinding,
																   rootEntityDescriptor, creationContext );
		this.discriminatorDescriptor = interpretDiscriminatorDescriptor( this, rootEntityBinding, creationContext );
		this.versionDescriptor = interpretVersionDescriptor( this, rootEntityBinding, creationContext );
		this.rowIdDescriptor = interpretRowIdDescriptor( this, rootEntityBinding, creationContext );
		this.tenantDiscrimination = interpretTenantDiscrimination( this, rootEntityBinding, creationContext );
		this.naturalIdentifierDescriptor = interpretNaturalIdentifierDescriptor( this, rootEntityBinding, creationContext );

		this.whereFragment = rootEntityBinding.getWhere();
		this.mutable = rootEntityBinding.isMutable();
		this.implicitPolymorphismEnabled = !rootEntityBinding.isExplicitPolymorphism();
	}

	private RepresentationMode determineRepresentationMode(
			RootClass rootEntityBinding,
			EntityDescriptor rootEntityPersister,
			RuntimeModelCreationContext creationContext) {
		// see if a specific one was requested specific to this hierarchy
		if ( rootEntityBinding.getExplicitRepresentationMode() != null ) {
			return rootEntityBinding.getExplicitRepresentationMode();
		}

		// otherwise,
		//
		// if there is no corresponding Java type, assume MAP mode
		if ( rootEntityPersister.getJavaTypeDescriptor().getJavaType() == null ) {
			return RepresentationMode.MAP;
		}


		// assume POJO
		return RepresentationMode.POJO;
	}

	private static InheritanceStrategy interpretInheritanceType(RootClass rootEntityBinding) {
		if ( rootEntityBinding.getSubTypeMappings().isEmpty() ) {
			return InheritanceStrategy.NONE;
		}
		else {
			final Subclass subEntityBinding = (Subclass) rootEntityBinding.getSubTypeMappings().iterator().next();
			if ( subEntityBinding instanceof UnionSubclass ) {
				return InheritanceStrategy.UNION;
			}
			else if ( subEntityBinding instanceof JoinedSubclass ) {
				return InheritanceStrategy.JOINED;
			}
			else {
				return InheritanceStrategy.DISCRIMINATOR;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static EntityIdentifier interpretIdentifierDescriptor(
			EntityHierarchyImpl runtimeModelHierarchy,
			RootClass bootModelRootEntity,
			EntityDescriptor runtimeModelRootEntity,
			RuntimeModelCreationContext creationContext) {
		if ( bootModelRootEntity.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping() != null ) {

			// should mean we have a "non-aggregated composite-id" (what we
			// 		historically called an "embedded composite id")
			return new EntityIdentifierCompositeNonAggregatedImpl(
					runtimeModelHierarchy,
					( (EmbeddedValueMappingImplementor) bootModelRootEntity.getIdentifier() ).makeRuntimeDescriptor(
							runtimeModelRootEntity,
							bootModelRootEntity.getIdentifierProperty().getName(),
							SingularPersistentAttribute.Disposition.ID,
							creationContext
					),
					bootModelRootEntity.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping()
			);
		}
		else if ( bootModelRootEntity.getIdentifier() instanceof EmbeddedValueMappingImplementor ) {
			// indicates we have an aggregated composite identifier (should)
			assert !bootModelRootEntity.getIdentifierProperty().isOptional();

			return  new EntityIdentifierCompositeAggregatedImpl(
					runtimeModelHierarchy,
					bootModelRootEntity,
					( (EmbeddedValueMappingImplementor) bootModelRootEntity.getIdentifier() ).makeRuntimeDescriptor(
							runtimeModelHierarchy.getRootEntityType(),
							bootModelRootEntity.getIdentifierProperty().getName(),
							SingularPersistentAttribute.Disposition.ID,
							creationContext
					),
					creationContext
			);
		}
		else {
			// should indicate a simple identifier
			assert !bootModelRootEntity.getIdentifierProperty().isOptional();

			return new EntityIdentifierSimpleImpl(
					runtimeModelHierarchy,
					bootModelRootEntity,
					creationContext
			);
		}
	}

	private static RowIdDescriptor interpretRowIdDescriptor(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		if ( rootEntityBinding.getRootTable().getRowId() != null ) {
			return new RowIdDescriptorImpl( hierarchy, creationContext );
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private static DiscriminatorDescriptor interpretDiscriminatorDescriptor(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		creationContext.getDatabaseObjectResolver().resolveTable( rootEntityBinding.getRootTable() );
		if ( rootEntityBinding.getDiscriminator() == null ) {
			return null;
		}

		return new DiscriminatorDescriptorImpl(
				hierarchy,
				(BasicValueMapping) rootEntityBinding.getDiscriminator(),
				creationContext
		);

	}

	@SuppressWarnings("unchecked")
	private static VersionDescriptor interpretVersionDescriptor(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		if ( rootEntityBinding.getVersion() == null ) {
			return null;
		}

		return new VersionDescriptorImpl(
				hierarchy,
				rootEntityBinding,
				creationContext
		);
	}

	private static TenantDiscrimination interpretTenantDiscrimination(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		return null;
	}

	private static NaturalIdDescriptor interpretNaturalIdentifierDescriptor(
			EntityHierarchyImpl entityHierarchy,
			RootClass rootEntityMapping,
			RuntimeModelCreationContext creationContext) {
		if ( !rootEntityMapping.hasNaturalId() ) {
			return null;
		}

		return new NaturalIdDescriptorImpl(
				entityHierarchy.getRootEntityType().getFactory().getCache().getNaturalIdRegionAccess( entityHierarchy )
		);
	}


	@Override
	public void finishInitialization(RuntimeModelCreationContext creationContext, RootClass mappingType) {
		if ( mappingType.hasNaturalId() ) {
			final List<NonIdPersistentAttribute> attributes = new ArrayList<>();
			for ( Property property : mappingType.getDeclaredProperties() ) {
				if ( property.isNaturalIdentifier() ) {
					final NonIdPersistentAttribute<?, ?> runtimeAttribute =
							rootEntityDescriptor.findPersistentAttribute( property.getName() );

					if ( !SingularPersistentAttribute.class.isInstance( runtimeAttribute ) ) {
						throw new HibernateException(
								"Attempt to define non-singular attribute [" + property.getName() +
										"] as part of the entity's natural-id : " + rootEntityDescriptor.getEntityName()
						);
					}
					attributes.add( runtimeAttribute );
				}
			}
			( (NaturalIdDescriptorImpl) naturalIdentifierDescriptor ).injectAttributes( attributes );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityDescriptor getRootEntityType() {
		return rootEntityDescriptor;
	}

	@Override
	public RepresentationMode getRepresentation() {
		return representationMode;
	}

	@Override
	public InheritanceStrategy getInheritanceStrategy() {
		return inheritanceStrategy;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityIdentifier getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public DiscriminatorDescriptor getDiscriminatorDescriptor() {
		return discriminatorDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public VersionDescriptor getVersionDescriptor() {
		return versionDescriptor;
	}

	@Override
	public NaturalIdDescriptor getNaturalIdDescriptor() {
		return naturalIdentifierDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RowIdDescriptor getRowIdDescriptor() {
		return rowIdDescriptor;
	}

	@Override
	public TenantDiscrimination getTenantDiscrimination() {
		return tenantDiscrimination;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public EntityDataAccess getEntityCacheAccess() {
		if ( caching == null ) {
			caching = rootEntityDescriptor.getFactory().getCache().getEntityRegionAccess( this );
		}
		return caching;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isImplicitPolymorphismEnabled() {
		return implicitPolymorphismEnabled;
	}

	@Override
	public String getWhere() {
		return whereFragment;
	}
}
