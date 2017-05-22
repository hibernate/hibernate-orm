/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import java.util.Iterator;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.InheritanceStrategy;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private static final Logger log = Logger.getLogger( EntityHierarchyImpl.class );

	private final EntityTypeImplementor rootEntityPersister;
	private final EntityRegionAccessStrategy caching;
	private final NaturalIdRegionAccessStrategy naturalIdCaching;

	private final InheritanceStrategy inheritanceStrategy;
	private final EntityMode entityMode;
	private final OptimisticLockStyle optimisticLockStyle;

	private final EntityIdentifier identifierDescriptor;
	private final RowIdDescriptor rowIdDescriptor;
	private final DiscriminatorDescriptor discriminatorDescriptor;
	private final VersionDescriptor versionDescriptor;
	private final TenantDiscrimination tenantDiscrimination;

	private final String whereFragment;
	private final boolean mutable;
	private final boolean implicitPolymorphismEnabled;

	public EntityHierarchyImpl(
			RuntimeModelCreationContext creationContext,
			EntityTypeImplementor rootEntityPersister,
			RootClass rootEntityBinding,
			EntityRegionAccessStrategy caching,
			NaturalIdRegionAccessStrategy naturalIdCaching) {
		log.debugf( "Creating EntityHierarchy root EntityPersister : %s", rootEntityPersister );

		this.rootEntityPersister = rootEntityPersister;
		this.caching = caching;
		this.naturalIdCaching = naturalIdCaching;
		this.inheritanceStrategy = interpretInheritanceType( rootEntityBinding );
		this.entityMode = rootEntityBinding.hasPojoRepresentation() ? EntityMode.POJO : EntityMode.MAP;
		this.optimisticLockStyle = rootEntityBinding.getOptimisticLockStyle();

		final Table identifierTable = resolveIdentifierTable( creationContext, rootEntityBinding );
		this.identifierDescriptor = interpretIdentifierDescriptor( this, creationContext, rootEntityBinding, rootEntityPersister, identifierTable );
		this.rowIdDescriptor = interpretRowIdDescriptor( this, creationContext, rootEntityBinding, identifierTable );
		this.discriminatorDescriptor = interpretDiscriminatorDescriptor( this, creationContext, rootEntityBinding, identifierTable );
		this.versionDescriptor = interpretVersionDescriptor( this, creationContext, rootEntityBinding, identifierTable );
		this.tenantDiscrimination = interpretTenantDiscrimination( this, creationContext, rootEntityBinding, identifierTable );

		this.whereFragment = rootEntityBinding.getWhere();
		this.mutable = rootEntityBinding.isMutable();
		this.implicitPolymorphismEnabled = !rootEntityBinding.isExplicitPolymorphism();
	}

	private static InheritanceStrategy interpretInheritanceType(RootClass rootEntityBinding) {
		if ( !rootEntityBinding.hasSubclasses() ) {
			return InheritanceStrategy.NONE;
		}
		else {
			final Subclass subEntityBinding = (Subclass) rootEntityBinding.getDirectSubclasses().next();
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

	private static Table resolveIdentifierTable(RuntimeModelCreationContext creationContext, RootClass rootEntityBinding) {
		return creationContext.getDatabaseObjectResolver().resolveTable( rootEntityBinding.getIdentityTable() );
	}

	@SuppressWarnings("unchecked")
	private static EntityIdentifier interpretIdentifierDescriptor(
			EntityHierarchyImpl hierarchy,
			RuntimeModelCreationContext creationContext,
			RootClass rootEntityBinding,
			EntityTypeImplementor rootEntityPersister,
			Table identifierTable) {
		if ( rootEntityBinding.getIdentifierMapper() != null ) {
			// should mean we have a "non-aggregated composite-id" (what we
			// 		historically called an "embedded composite id")
			return new EntityIdentifierCompositeNonAggregatedImpl(
					hierarchy,
					creationContext.getPersisterFactory().createEmbeddablePersister(
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
					creationContext.getPersisterFactory().createEmbeddablePersister(
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
					(BasicType) rootEntityBinding.getIdentifier().getType(),
					resolveColumns( identifierTable, rootEntityBinding.getIdentifier(), creationContext ),
					creationContext
			);
		}
	}

	private static IdentifiableTypeImplementor resolveIdAttributeDeclarer(
			RootClass rootEntityBinding,
			EntityTypeImplementor rootEntityPersister) {
		// for now assume the root entity as the declarer
		return rootEntityPersister;
	}

	private static List<Column> resolveColumns(
			Table table,
			Value value,
			RuntimeModelCreationContext creationContext) {
		final String[] columnNames = new String[ value.getColumnSpan() ];
		final String[] formulas = value.hasFormula() ? new String[ value.getColumnSpan() ] : null;

		final Iterator<Selectable> itr = value.getColumnIterator();
		int i = 0;
		while ( itr.hasNext() ) {
			final Selectable selectable = itr.next();
			if ( selectable instanceof org.hibernate.mapping.Column ) {
				columnNames[i] = ( (org.hibernate.mapping.Column) selectable ).getQuotedName(
						creationContext.getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect()
				);
			}
			else {
				if ( formulas == null ) {
					throw new HibernateException( "Value indicated it does not have formulas, but a formula was encountered : " + selectable );
				}
				formulas[i] = ( (Formula) selectable ).getFormula();
			}

			// todo : keep track of readers/writers... how exactly?
			// something like this vv ?
			//		Column#applyReadExpression( col.getReadExpr( dialect ) )
			//		Column#applyWriteExpression( col.getWriteExpr() )

			i++;
		}

		return PersisterHelper.makeValues(
				creationContext.getSessionFactory(),
				value.getType(),
				columnNames,
				formulas,
				table
		);
	}

	private static RowIdDescriptor interpretRowIdDescriptor(
			EntityHierarchyImpl hierarchy,
			RuntimeModelCreationContext creationContext,
			RootClass rootEntityBinding,
			Table identifierTable) {
		if ( rootEntityBinding.getRootTable().getRowId() != null ) {
			return new RowIdDescriptorImpl( hierarchy );
		}

		return null;
	}

	private static DiscriminatorDescriptor interpretDiscriminatorDescriptor(
			EntityHierarchyImpl hierarchy,
			RuntimeModelCreationContext creationContext,
			RootClass rootEntityBinding,
			Table identifierTable) {
		if ( rootEntityBinding.getDiscriminator() == null ) {
			return null;
		}

		final List<Column> columns = resolveColumns( identifierTable, rootEntityBinding.getDiscriminator(), creationContext );
		if ( columns.size() > 1 ) {
			throw new MappingException( "Entity discriminator defined multiple columns : " + rootEntityBinding.getEntityName() );
		}
		return new DiscriminatorDescriptorImpl(
				hierarchy,
				(BasicType) rootEntityBinding.getDiscriminator().getType(),
				columns.get( 0 )
		);

	}

	private static VersionDescriptor interpretVersionDescriptor(
			EntityHierarchyImpl hierarchy,
			RuntimeModelCreationContext creationContext,
			RootClass rootEntityBinding,
			Table identifierTable) {
		if ( rootEntityBinding.getVersion() == null ) {
			return null;
		}

		final List<Column> columns = resolveColumns( identifierTable, rootEntityBinding.getVersion().getValue(), creationContext );
		if ( columns.size() > 1 ) {
			throw new MappingException( "Entity discriminator defined multiple columns : " + rootEntityBinding.getEntityName() );
		}

		return new VersionDescriptorImpl(
				hierarchy,
				rootEntityBinding,
				columns.get( 0 ),
				rootEntityBinding.getVersion().getName(),
				(BasicType) rootEntityBinding.getVersion().getType(),
				false,
				( (KeyValue) rootEntityBinding.getVersion().getValue() ).getNullValue(),
				creationContext
		);
	}

	private static TenantDiscrimination interpretTenantDiscrimination(
			EntityHierarchyImpl hierarchy,
			RuntimeModelCreationContext creationContext,
			RootClass rootEntityBinding,
			Table identifierTable) {
		return null;
	}


	@Override
	public void finishInitialization(RuntimeModelCreationContext creationContext, RootClass mappingType) {
		// anything to do?
	}

	@Override
	public EntityTypeImplementor getRootEntityType() {
		return rootEntityPersister;
	}

	@Override
	public InheritanceStrategy getInheritanceStrategy() {
		return inheritanceStrategy;
	}

	@Override
	public EntityMode getRepresentationMode() {
		return entityMode;
	}

	@Override
	public EntityIdentifier getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	@Override
	public RowIdDescriptor getRowIdDescriptor() {
		return rowIdDescriptor;
	}

	@Override
	public DiscriminatorDescriptor getDiscriminatorDescriptor() {
		return discriminatorDescriptor;
	}

	@Override
	public VersionDescriptor getVersionDescriptor() {
		return versionDescriptor;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public TenantDiscrimination getTenantDiscrimination() {
		return tenantDiscrimination;
	}

	@Override
	public EntityRegionAccessStrategy getEntityRegionAccessStrategy() {
		return caching;
	}

	@Override
	public NaturalIdRegionAccessStrategy getNaturalIdRegionAccessStrategy() {
		return naturalIdCaching;
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
