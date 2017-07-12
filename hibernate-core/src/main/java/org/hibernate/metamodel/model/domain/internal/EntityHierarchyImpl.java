/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import java.util.Collection;

import org.hibernate.EntityMode;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.Representation;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.InheritanceStrategy;
import org.hibernate.metamodel.model.domain.spi.NaturalIdentifierDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private static final Logger log = Logger.getLogger( EntityHierarchyImpl.class );

	private final EntityDescriptor rootEntityPersister;
	private final EntityRegionAccessStrategy caching;

	private final InheritanceStrategy inheritanceStrategy;
	private final EntityMode entityMode;
	private final OptimisticLockStyle optimisticLockStyle;

	private final EntityIdentifier identifierDescriptor;
	private final DiscriminatorDescriptor discriminatorDescriptor;
	private final VersionDescriptor versionDescriptor;
	private final NaturalIdentifierDescriptor naturalIdentifierDescriptor;
	private final RowIdDescriptor rowIdDescriptor;
	private final TenantDiscrimination tenantDiscrimination;

	private final String whereFragment;
	private final boolean mutable;
	private final boolean implicitPolymorphismEnabled;

	public EntityHierarchyImpl(
			RuntimeModelCreationContext creationContext,
			EntityDescriptor rootEntityPersister,
			RootClass rootEntityBinding,
			EntityRegionAccessStrategy caching,
			NaturalIdRegionAccessStrategy naturalIdCaching) {
		log.debugf( "Creating EntityHierarchy root EntityPersister : %s", rootEntityPersister );

		this.rootEntityPersister = rootEntityPersister;
		this.caching = caching;
		this.inheritanceStrategy = interpretInheritanceType( rootEntityBinding );
		this.entityMode = rootEntityBinding.getEntityMappingHierarchy().getEntityMode();
		this.optimisticLockStyle = rootEntityBinding.getEntityMappingHierarchy().getOptimisticLockStyle();

		this.identifierDescriptor = interpretIdentifierDescriptor( this, rootEntityBinding, rootEntityPersister, creationContext );
		this.discriminatorDescriptor = interpretDiscriminatorDescriptor( this, rootEntityBinding, creationContext );
		this.versionDescriptor = interpretVersionDescriptor( this, rootEntityBinding, creationContext );
		this.rowIdDescriptor = interpretRowIdDescriptor( this, rootEntityBinding, creationContext );
		this.tenantDiscrimination = interpretTenantDiscrimination( this, rootEntityBinding, creationContext );
		this.naturalIdentifierDescriptor = interpretNaturalIdentifierDescriptor( this, rootEntityBinding, creationContext );

		this.whereFragment = rootEntityBinding.getWhere();
		this.mutable = rootEntityBinding.isMutable();
		this.implicitPolymorphismEnabled = !rootEntityBinding.isExplicitPolymorphism();
	}

	private static InheritanceStrategy interpretInheritanceType(RootClass rootEntityBinding) {
		if ( !rootEntityBinding.getSubTypeMappings().isEmpty() ) {
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
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			EntityDescriptor rootEntityPersister,
			RuntimeModelCreationContext creationContext) {
		if ( rootEntityBinding.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping() != null ) {
			// should mean we have a "non-aggregated composite-id" (what we
			// 		historically called an "embedded composite id")
			return new EntityIdentifierCompositeNonAggregatedImpl(
					hierarchy,
					creationContext.getRuntimeModelDescriptorFactory().createEmbeddedTypeDescriptor(
							(Component) rootEntityBinding.getIdentifier(),
							resolveIdAttributeDeclarer( rootEntityBinding, rootEntityPersister ),
							rootEntityBinding.getIdentifierProperty().getName(),
							creationContext
					)
			);
		}
		else if ( rootEntityBinding.getIdentifier() instanceof Component ) {
			// indicates we have an aggregated composite identifier (should)
			return  new EntityIdentifierCompositeAggregatedImpl(
					hierarchy,
					rootEntityBinding.getIdentifierProperty(),
					creationContext.getRuntimeModelDescriptorFactory().createEmbeddedTypeDescriptor(
							(Component) rootEntityBinding.getIdentifier(),
							resolveIdAttributeDeclarer( rootEntityBinding, rootEntityPersister ),
							rootEntityBinding.getIdentifierProperty().getName(),
							creationContext
					),
					creationContext
			);
		}
		else {
			// should indicate a simple identifier
			return new EntityIdentifierSimpleImpl(
					hierarchy,
					resolveIdAttributeDeclarer( rootEntityBinding, rootEntityPersister ),
					rootEntityBinding.getIdentifierProperty(),
					(BasicValueMapping) rootEntityBinding.getIdentifier(),
					creationContext
			);
		}
	}

	private static IdentifiableTypeDescriptor resolveIdAttributeDeclarer(
			RootClass rootEntityBinding,
			EntityDescriptor rootEntityPersister) {
		// for now assume the root entity as the declarer
		return rootEntityPersister;
	}

	private static RowIdDescriptor interpretRowIdDescriptor(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		if ( rootEntityBinding.getRootTable().getRowId() != null ) {
			return new RowIdDescriptorImpl( hierarchy );
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
				rootEntityBinding.getVersion().getName(),
				false,
				(BasicValueMapping) rootEntityBinding.getVersion().getValue(),
				( (KeyValue) rootEntityBinding.getVersion().getValue() ).getNullValue(),
				creationContext
		);
	}

	private static TenantDiscrimination interpretTenantDiscrimination(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		return null;
	}

	private NaturalIdentifierDescriptor interpretNaturalIdentifierDescriptor(
			EntityHierarchyImpl entityHierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		if ( !rootEntityBinding.hasNaturalId() ) {
			return null;
		}

		final NaturalIdRegionAccessStrategy accessStrategy = creationContext.getSessionFactory()
				.getCache()
				.determineNaturalIdRegionAccessStrategy( rootEntityBinding );

		return new NaturalIdentifierDescriptor() {
			@Override
			public Collection<PersistentAttribute> getPersistentAttributes() {
				throw new NotYetImplementedException(  );
			}

			@Override
			public Object[] resolveSnapshot(Object entityId, SharedSessionContractImplementor session) {
				return new Object[0];
			}

			@Override
			public NaturalIdRegionAccessStrategy getNaturalIdRegionAccessStrategy() {
				return accessStrategy;
			}
		};
	}


	@Override
	public void finishInitialization(RuntimeModelCreationContext creationContext, RootClass mappingType) {
		// anything to do?
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityDescriptor getRootEntityType() {
		return rootEntityPersister;
	}

	@Override
	public InheritanceStrategy getInheritanceStrategy() {
		return inheritanceStrategy;
	}

	@Override
	public Representation getRepresentation() {
		return entityMode.asRepresentation();
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
	public NaturalIdentifierDescriptor getNaturalIdentifierDescriptor() {
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
	public EntityRegionAccessStrategy getEntityRegionAccessStrategy() {
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
