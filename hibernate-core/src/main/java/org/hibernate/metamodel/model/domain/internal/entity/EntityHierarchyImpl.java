/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal.entity;

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
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.InheritanceStrategy;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.type.descriptor.java.spi.EntityMutabilityPlan;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private static final Logger log = Logger.getLogger( EntityHierarchyImpl.class );

	private final EntityTypeDescriptor<?> rootEntityDescriptor;

	private final InheritanceStrategy inheritanceStrategy;
	private final OptimisticLockStyle optimisticLockStyle;
	private final RepresentationMode representationMode;

	private final EntityIdentifier identifierDescriptor;
	private final DiscriminatorDescriptor discriminatorDescriptor;
	private final VersionDescriptor versionDescriptor;
	private final NaturalIdDescriptor naturalIdentifierDescriptor;
	private final RowIdDescriptor rowIdDescriptor;
	private final TenantDiscrimination tenantDiscrimination;

	private EntityDataAccess caching;

	private final EntityMutabilityPlan mutabilityPlan;

	private final boolean implicitPolymorphismEnabled;

	private final String whereFragment;

	public EntityHierarchyImpl(
			EntityTypeDescriptor rootRuntimeDescriptor,
			RootClass rootBootDescriptor,
			RuntimeModelCreationContext creationContext) {
		log.debugf( "Creating EntityHierarchy root EntityPersister : %s", rootRuntimeDescriptor );

		this.rootEntityDescriptor = rootRuntimeDescriptor;

		this.inheritanceStrategy = interpretInheritanceType( rootBootDescriptor );

		this.identifierDescriptor = interpretIdentifierDescriptor(
				this,
				rootBootDescriptor,
				rootRuntimeDescriptor,
				creationContext
		);
		this.discriminatorDescriptor = interpretDiscriminatorDescriptor(
				this,
				rootBootDescriptor,
				creationContext
		);
		this.versionDescriptor = interpretVersionDescriptor(
				this,
				rootBootDescriptor,
				creationContext
		);
		this.rowIdDescriptor = interpretRowIdDescriptor(
				this,
				rootBootDescriptor,
				creationContext
		);
		this.tenantDiscrimination = interpretTenantDiscrimination(
				this,
				rootBootDescriptor,
				creationContext
		);
		this.naturalIdentifierDescriptor = interpretNaturalIdentifierDescriptor(
				this,
				rootBootDescriptor,
				creationContext
		);

		this.representationMode = determineRepresentationMode(
				rootBootDescriptor,
				rootRuntimeDescriptor,
				creationContext
		);

		this.implicitPolymorphismEnabled = !rootBootDescriptor.isExplicitPolymorphism();
		this.optimisticLockStyle = rootBootDescriptor.getEntityMappingHierarchy().getOptimisticLockStyle();
		this.mutabilityPlan = (EntityMutabilityPlan) rootBootDescriptor.getJavaTypeMapping()
				.getJavaTypeDescriptor()
				.getMutabilityPlan();
		this.whereFragment = rootBootDescriptor.getWhere();
	}

	private static RepresentationMode determineRepresentationMode(
			RootClass rootEntityBinding,
			EntityTypeDescriptor rootEntityDescriptor,
			RuntimeModelCreationContext creationContext) {
		// see if a specific one was requested specific to this hierarchy
		if ( rootEntityBinding.getExplicitRepresentationMode() != null ) {
			return rootEntityBinding.getExplicitRepresentationMode();
		}

		// otherwise,
		//
		// if there is no corresponding Java type, assume MAP mode
		if ( rootEntityDescriptor.getJavaTypeDescriptor().getJavaType() == null ) {
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
			EntityTypeDescriptor runtimeModelRootEntity,
			RuntimeModelCreationContext creationContext) {
		if ( bootModelRootEntity.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping() != null ) {

			// should mean we have a "non-aggregated composite-id" (what we
			// 		historically called an "embedded composite id")
			return new EntityIdentifierCompositeNonAggregatedImpl(
					runtimeModelHierarchy,
					( (EmbeddedValueMappingImplementor) bootModelRootEntity.getIdentifier() ).makeRuntimeDescriptor(
							runtimeModelRootEntity,
							bootModelRootEntity.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping().getName(),
							SingularPersistentAttribute.Disposition.ID,
							creationContext
					),
					bootModelRootEntity.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping()
			);
		}
		else if ( bootModelRootEntity.getIdentifier() instanceof EmbeddedValueMappingImplementor ) {
			// indicates we have an aggregated composite identifier (should)
			assert !bootModelRootEntity.getIdentifierAttributeMapping().isOptional();

			return  new EntityIdentifierCompositeAggregatedImpl(
					runtimeModelHierarchy,
					bootModelRootEntity,
					( (EmbeddedValueMappingImplementor) bootModelRootEntity.getIdentifier() ).makeRuntimeDescriptor(
							runtimeModelHierarchy.getRootEntityType(),
							bootModelRootEntity.getIdentifierAttributeMapping().getName(),
							SingularPersistentAttribute.Disposition.ID,
							creationContext
					),
					creationContext
			);
		}
		else {
			// should indicate a simple identifier
			assert !bootModelRootEntity.getIdentifierAttributeMapping().isOptional();

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
		if ( rootEntityBinding.getVersionAttributeMapping() == null ) {
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
				entityHierarchy,
				entityHierarchy.getRootEntityType()
						.getFactory()
						.getCache()
						.getNaturalIdCacheRegionAccessStrategy( entityHierarchy.rootEntityDescriptor.getNavigableRole() )
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
	public EntityTypeDescriptor getRootEntityType() {
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
			caching = rootEntityDescriptor.getFactory().getCache().getEntityRegionAccess( rootEntityDescriptor.getNavigableRole()  );
		}
		return caching;
	}

	@Override
	public EntityMutabilityPlan getMutabilityPlan() {
		return mutabilityPlan;
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
