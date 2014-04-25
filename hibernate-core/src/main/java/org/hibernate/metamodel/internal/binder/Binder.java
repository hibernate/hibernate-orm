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
package org.hibernate.metamodel.internal.binder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.TruthValue;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.binder.HibernateTypeHelper.ReflectedCollectionJavaTypes;
import org.hibernate.metamodel.internal.resolver.AssociationRelationalBindingResolver;
import org.hibernate.metamodel.internal.resolver.MappedByAssociationRelationalBindingResolverImpl;
import org.hibernate.metamodel.internal.resolver.StandardAssociationRelationalBindingResolverImpl;
import org.hibernate.metamodel.reflite.internal.DynamicTypeDescriptorImpl;
import org.hibernate.metamodel.reflite.internal.Primitives;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.source.spi.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.ConstraintSource;
import org.hibernate.metamodel.source.spi.DerivedValueSource;
import org.hibernate.metamodel.source.spi.DiscriminatorSource;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.source.spi.EmbeddedAttributeSource;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.FilterSource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.IndexConstraintSource;
import org.hibernate.metamodel.source.spi.IndexedPluralAttributeSource;
import org.hibernate.metamodel.source.spi.JoinedSubclassEntitySource;
import org.hibernate.metamodel.source.spi.MappedByAssociationSource;
import org.hibernate.metamodel.source.spi.MapsIdSource;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.source.spi.MultiTenancySource;
import org.hibernate.metamodel.source.spi.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.Orderable;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceBasic;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceEmbedded;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceOneToMany;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.PluralAttributeMapKeySourceBasic;
import org.hibernate.metamodel.source.spi.PluralAttributeMapKeySourceEmbedded;
import org.hibernate.metamodel.source.spi.PluralAttributeMapKeySourceEntityAttribute;
import org.hibernate.metamodel.source.spi.PluralAttributeSequentialIndexSource;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.RelationalValueSourceContainer;
import org.hibernate.metamodel.source.spi.SecondaryTableSource;
import org.hibernate.metamodel.source.spi.SimpleIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.Sortable;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.source.spi.UniqueConstraintSource;
import org.hibernate.metamodel.source.spi.VersionAttributeSource;
import org.hibernate.metamodel.spi.AbstractPersistentAttributeMemberResolver;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.SingularAttributeNature;
import org.hibernate.metamodel.spi.binding.AbstractEmbeddableBinding;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.Cascadeable;
import org.hibernate.metamodel.spi.binding.CompositePluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBindingImplementor;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.EntityVersion;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.HierarchyDetails;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.binding.OneToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingBasic;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingEmbedded;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingManyToMany;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingOneToMany;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SetBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.BasicType;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.IdentifiableType;
import org.hibernate.metamodel.spi.domain.IndexedPluralAttribute;
import org.hibernate.metamodel.spi.domain.MappedSuperclass;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.tuple.component.ComponentTuplizer;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import static org.hibernate.MultiTenancyStrategy.DISCRIMINATOR;
import static org.hibernate.engine.spi.SyntheticAttributeHelper.SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME;
import static org.hibernate.metamodel.spi.binding.EntityIdentifier.NonAggregatedCompositeIdentifierBinding;

/**
 * The common binder shared between annotations and {@code hbm.xml} processing.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 * @author Brett Meyer
 * @author Strong Liu
 */
public class Binder {
	private static final Logger LOG = Logger.getLogger( Binder.class );

	private final SourceIndex sourceIndex;

	private final BinderEventBus eventBus;
	private final BinderRootContextImpl rootBindingContext;
	private final BinderProcessHelper processHelper;

	private final JavaTypeDescriptorRepository javaTypeDescriptorRepository;

	private final StandardAssociationRelationalBindingResolverImpl standardAssociationRelationalBindingResolver;
	private final MappedByAssociationRelationalBindingResolverImpl mappedByAssociationRelationalBindingResolver;

	private final Map<String,CacheRegionDefinition> entityCacheRegionDefinitionsByEntityName = new HashMap<String, CacheRegionDefinition>();


	public Binder(BindingContext rootBindingContext) {
		this.eventBus = new BinderEventBus();
		this.sourceIndex = new SourceIndex( rootBindingContext );

		this.rootBindingContext = new BinderRootContextImpl( rootBindingContext, sourceIndex, eventBus );

		this.processHelper = new BinderProcessHelper( this.rootBindingContext );

		this.standardAssociationRelationalBindingResolver =
				new StandardAssociationRelationalBindingResolverImpl( this.rootBindingContext );
		this.mappedByAssociationRelationalBindingResolver =
				new MappedByAssociationRelationalBindingResolverImpl( this.rootBindingContext );

		this.javaTypeDescriptorRepository = this.rootBindingContext.getJavaTypeDescriptorRepository();

		// index cache region definitions for easier lookup.
		// for now, just entity regions
		if ( rootBindingContext.getBuildingOptions().getCacheRegionDefinitions() != null ) {
			for ( CacheRegionDefinition cacheRegionDefinition
					: rootBindingContext.getBuildingOptions().getCacheRegionDefinitions() ) {
				if ( cacheRegionDefinition.getRegionType() == CacheRegionDefinition.CacheRegionType.ENTITY ) {
					entityCacheRegionDefinitionsByEntityName.put(
							cacheRegionDefinition.getRole(),
							cacheRegionDefinition
					);
				}
			}
		}
	}

	/**
	 * The entry point of {@linkplain Binder} class, adds all the entity hierarchy one by one.
	 *
	 * Indexes all {@link EntitySource} objects in an {@link org.hibernate.metamodel.source.spi.EntityHierarchySource} and
	 * creates all {@link EntityBinding}.
	 *
	 * @param entityHierarchies The entity hierarchies resolved from mappings
	 */
	public void addEntityHierarchies(final Collection<EntityHierarchySource> entityHierarchies) {
		// todo : ultimately (unified source) pass all hierarchies to SourceIndex at once..

		processHelper.apply( entityHierarchies, new PreliminarySourceHandlingStep() );
	}

	public class PreliminarySourceHandlingStep implements BinderStepHierarchyStrategy {
		@Override
		public void visit(EntityHierarchySource source, BinderLocalBindingContext context) {
			sourceIndex.indexHierarchy( source );

			final HierarchyDetails hierarchyDetails = createHierarchyDetails( source );
			rootBindingContext.addMapping( source, hierarchyDetails );

			final EntityBinding rootEntityBinding = hierarchyDetails.getRootEntityBinding();
			final EntitySource rootEntitySource = source.getRoot();
			preliminaryEntityBinding( rootEntityBinding, rootEntitySource, hierarchyDetails );

			bindVersion( hierarchyDetails, source );
			bindDiscriminator( hierarchyDetails, source );
			bindMultiTenancy( hierarchyDetails, source );

			if ( hierarchyDetails.getInheritanceType() != InheritanceType.NO_INHERITANCE ) {
				// apply to persistent subclasses
				visitPreliminaryEntityBindingOnSubclasses( rootEntityBinding, rootEntitySource, hierarchyDetails );
			}
		}

		private HierarchyDetails createHierarchyDetails(EntityHierarchySource hierarchySource) {
			final HierarchyDetails.Builder builder = new HierarchyDetails.Builder()
					.setInheritanceType( hierarchySource.getHierarchyInheritanceType() )
					.setDiscriminated( hierarchySource.getDiscriminatorSource() != null )
					.setVersioned( hierarchySource.getVersionAttributeSource() != null )
					.setEntityMode( hierarchySource.getEntityMode() )
					.setMutable( hierarchySource.isMutable() )
					.setExplicitPolymorphism( hierarchySource.isExplicitPolymorphism() )
					.setOptimisticLockStyle( hierarchySource.getOptimisticLockStyle() )
					.setWhere( hierarchySource.getWhere() );

			// entity caching
			String entityName = hierarchySource.getRoot().getEntityName();
			Caching caching = hierarchySource.getCaching();
			if ( caching == null ) {
				caching = new Caching( TruthValue.UNKNOWN );
			}
			CacheRegionDefinition cachingOverride = entityCacheRegionDefinitionsByEntityName.get( entityName );
			if ( cachingOverride != null ) {
				caching.overlay( cachingOverride );
			}
			builder.setCaching( caching );

			// natural id caching
			Caching naturalIdCaching = hierarchySource.getNaturalIdCaching();
			if ( naturalIdCaching == null ) {
				naturalIdCaching = new Caching( TruthValue.UNKNOWN );
			}
			builder.setNaturalIdCaching( naturalIdCaching );

			// multi-tenancy discriminator
			if ( hierarchySource.getMultiTenancySource() != null ) {
				builder.setTenancyDiscriminated( !hierarchySource.getMultiTenancySource().isShared() );
			}

			return builder.createHierarchyDetails();
		}

		private void preliminaryEntityBinding(
				EntityBinding binding,
				EntitySource source,
				HierarchyDetails hierarchyDetails) {
			rootBindingContext.addMapping( source, binding );

			binding.setEntity( (Entity) resolveDomainType( source ) );
			binding.setEntityName( source.getEntityName() );
			binding.setJpaEntityName( source.getJpaEntityName() );
			binding.setDynamicUpdate( source.isDynamicUpdate() );
			binding.setDynamicInsert( source.isDynamicInsert() );
			binding.setBatchSize( source.getBatchSize() );
			binding.setSelectBeforeUpdate( source.isSelectBeforeUpdate() );
			binding.setAbstract( source.isAbstract() );
			binding.setCustomLoaderName( source.getCustomLoaderName() );
			binding.setCustomInsert( source.getCustomSqlInsert() );
			binding.setCustomUpdate( source.getCustomSqlUpdate() );
			binding.setCustomDelete( source.getCustomSqlDelete() );
			binding.setJpaCallbackClasses( source.getJpaCallbackClasses() );

			// todo: deal with joined and unioned subclass bindings
			// todo: bind fetch profiles
			// Configure rest of binding
			final String customTuplizerClassName = source.getCustomTuplizerClassName();
			if ( customTuplizerClassName != null ) {
				binding.setCustomEntityTuplizerClass(
						localBindingContext().getClassLoaderAccess().<EntityTuplizer>classForName(
								customTuplizerClassName
						)
				);
			}
			final String customPersisterClassName = source.getCustomPersisterClassName();
			if ( customPersisterClassName != null ) {
				binding.setCustomEntityPersisterClass(
						localBindingContext().getClassLoaderAccess().<EntityPersister>classForName(
								customPersisterClassName
						)
				);
			}
			binding.setMetaAttributeContext(
					createMetaAttributeContext(
							source.getToolingHintSources(),
							true,
							localBindingContext().getGlobalMetaAttributeContext()
					)
			);

			if ( source.getSynchronizedTableNames() != null ) {
				binding.addSynchronizedTableNames( source.getSynchronizedTableNames() );
			}

			resolveEntityLaziness( binding, source, hierarchyDetails.getEntityMode() );

			if ( source.getFilterSources() != null ) {
				for ( FilterSource filterSource : source.getFilterSources() ) {
					binding.addFilterConfiguration( createFilterConfiguration( filterSource, binding ) );
				}
			}

			bindPrimaryTable( binding, source );

			if ( binding.getHierarchyDetails().getEntityDiscriminator() != null ) {
				final String discriminatorValue = source.getDiscriminatorMatchValue();
				if ( discriminatorValue != null ) {
					binding.setDiscriminatorMatchValue( discriminatorValue );
				}
				else {
					// Use the class name as a default if no discriminator value.
					binding.setDiscriminatorMatchValue( StringHelper.unqualify( source.getEntityName() ) );
				}
			}

			// Register binding with metadata
			localBindingContext().getMetadataCollector().addEntity( binding );
		}

		private void visitPreliminaryEntityBindingOnSubclasses(
				EntityBinding binding,
				IdentifiableTypeSource source,
				HierarchyDetails hierarchyDetails) {
			for ( IdentifiableTypeSource subType : source.getSubTypes() ) {
				if ( EntitySource.class.isInstance( subType ) ) {
					final EntityBinding subBinding = binding.makeSubBinding();
					preliminaryEntityBinding( subBinding, (EntitySource) subType, hierarchyDetails );
					visitPreliminaryEntityBindingOnSubclasses( subBinding, subType, hierarchyDetails );
				}
				else {
					visitPreliminaryEntityBindingOnSubclasses( binding, subType, hierarchyDetails );
				}
			}
		}

		private void resolveEntityLaziness(
				final EntityBinding entityBinding,
				final EntitySource entitySource,
				final EntityMode entityMode) {
			if ( entityMode == EntityMode.POJO ) {
				final String proxy = entitySource.getProxy();
				if ( proxy == null ) {
					if ( entitySource.isLazy() ) {
						entityBinding.setProxyInterfaceType( entityBinding.getEntity().getDescriptor() );
						entityBinding.setLazy( true );
					}
				}
				else {
					entityBinding.setProxyInterfaceType(
							localBindingContext().typeDescriptor( proxy )
					);
					entityBinding.setLazy( true );
				}
			}
			else {
				entityBinding.setProxyInterfaceType( null );
				entityBinding.setLazy( entitySource.isLazy() );
			}
		}

		private void bindVersion(final HierarchyDetails hierarchyDetails, final EntityHierarchySource source) {
			final VersionAttributeSource versionAttributeSource = source.getVersionAttributeSource();

			if ( versionAttributeSource == null ) {
				return;
			}

			final EntityVersion version = hierarchyDetails.getEntityVersion();
			final BasicAttributeBinding versionAttributeBinding = (BasicAttributeBinding) bindAttribute(
					hierarchyDetails.getRootEntityBinding(),
					versionAttributeSource
			);
			version.setVersioningAttributeBinding( versionAttributeBinding );

			// Should be just one value.
			if ( versionAttributeBinding.getRelationalValueBindings().size() != 1 ) {
				throw source.getRoot().getLocalBindingContext().makeMappingException(
						"Encountered multi-column version attribute"
				);
			}

			// If it is a column, make sure the column is non-nullable
			final RelationalValueBinding valueBinding = versionAttributeBinding.getRelationalValueBindings()
					.get( 0 );
			if ( !valueBinding.isDerived() ) {
				( (Column) valueBinding.getValue() ).setNullable( false );
			}

			version.setUnsavedValue(
					versionAttributeSource.getUnsavedValue() == null
							? "undefined"
							: versionAttributeSource.getUnsavedValue()
			);
		}

		private void bindDiscriminator(HierarchyDetails binding, EntityHierarchySource source) {
			final DiscriminatorSource discriminatorSource = source.getDiscriminatorSource();
			if ( discriminatorSource == null ) {
				return;
			}

			binding.getEntityDiscriminator().setInserted( discriminatorSource.isInserted() );
			binding.getEntityDiscriminator().setForced( discriminatorSource.isForced() );

			final RelationalValueSource valueSource = discriminatorSource.getDiscriminatorRelationalValueSource();
			final TableSpecification table = binding.getRootEntityBinding().getPrimaryTable();
			if ( valueSource != null && valueSource.getContainingTableName() != null ) {
				// JPA says that the discriminator needs to come from the primary table...
				// if source specifically named a table for the discriminator, make sure it matches
				// the primary table
				// todo : implement check, or not?
			}

			final Value value = buildDiscriminatorRelationValue( valueSource, table );
			binding.getEntityDiscriminator().setRelationalValue( value );

			typeHelper().bindDiscriminatorType( binding.getEntityDiscriminator(), value );
		}

		private Value buildDiscriminatorRelationValue(
				final RelationalValueSource valueSource,
				final TableSpecification table) {
			if ( valueSource.getNature() == RelationalValueSource.Nature.COLUMN ) {
				return tableHelper().locateOrCreateColumn(
						table,
						(ColumnSource) valueSource,
						new ColumnNamingStrategyHelper(
								localBindingContext().getMappingDefaults().getDiscriminatorColumnName(),
								false
						),
						false,
						false
				);
			}
			else {
				return table.locateOrCreateDerivedValue( ( (DerivedValueSource) valueSource ).getExpression() );
			}
		}

		private void bindMultiTenancy(HierarchyDetails binding, EntityHierarchySource source) {
			final MultiTenancySource multiTenancySource = source.getMultiTenancySource();
			if ( multiTenancySource == null ) {
				return;
			}

			// if (1) the strategy is discriminator based and (2) the entity is not shared, we need to either (a) extract
			// the user supplied tenant discriminator value mapping or (b) generate an implicit one
			final MultiTenancyStrategy strategy = localBindingContext().getBuildingOptions().getMultiTenancyStrategy();
			if ( strategy != DISCRIMINATOR || multiTenancySource.isShared() ) {
				return;
			}

			// NOTE : the table for tenant identifier/discriminator is always the primary table
			final Value tenantDiscriminatorValue;
			final RelationalValueSource valueSource = multiTenancySource.getRelationalValueSource();
			final TableSpecification table = binding.getRootEntityBinding().getPrimaryTable();

			if ( valueSource == null ) {
				// user supplied no explicit information, so use implicit mapping with default name
				tenantDiscriminatorValue = table.locateOrCreateColumn(
						localBindingContext().getMappingDefaults().getTenantIdColumnName()
				);
			}
			else {
				tenantDiscriminatorValue = buildDiscriminatorRelationValue( valueSource, table );
			}

			binding.getTenantDiscrimination().setDiscriminatorValue( tenantDiscriminatorValue );
			binding.getTenantDiscrimination().setShared( multiTenancySource.isShared() );
			binding.getTenantDiscrimination().setUseParameterBinding( multiTenancySource.bindAsParameter() );
		}
	}


	/**
	 * Resolves the domain type give a source object representing an Entity or MappedSupperclass
	 *
	 * @param source The source object
	 *
	 * @return The Entity/MappedSuperclass
	 */
	@SuppressWarnings("unchecked")
	private IdentifiableType resolveDomainType(IdentifiableTypeSource source) {
		if ( source == null ) {
			return null;
		}

		final DotName descriptorTypeName = javaTypeDescriptorRepository.buildName(
				typeHelper().determineJavaTypeName( source )
		);
		final JavaTypeDescriptor javaTypeDescriptor = javaTypeDescriptorRepository.getType( descriptorTypeName );

		if ( EntitySource.class.isInstance( source ) ) {
			Entity entityDomainType = (Entity) localBindingContext().locateDomainType( javaTypeDescriptor );

			if ( entityDomainType == null ) {
				entityDomainType = localBindingContext().buildEntityDomainType(
						javaTypeDescriptor,
						resolveDomainType( source.getSuperType() )
				);
			}

			return entityDomainType;
		}
		else {
			MappedSuperclass superDomainType = (MappedSuperclass) localBindingContext().locateDomainType(
					javaTypeDescriptor
			);

			if ( superDomainType == null ) {
				superDomainType = localBindingContext().buildMappedSuperclassDomainType(
						javaTypeDescriptor,
						resolveDomainType( source.getSuperType() )
				);
			}

			return superDomainType;
		}
	}

	public void bindEntityHierarchies() {
		// Bind everything except for (non-ID) attributes.
		// Need to bind ID attributes before resolving associations.
		// TODO: when we know the proper order for processing entity hierarchies,
		//       then applyToAllEntityHierarchies(...) can replace the following method.
		bindEntityHierarchiesExcludingNonIdAttributeBindings();

		// Bind identifier generator for root entitybinding, we have to wait for all entiybindings created,
		// since for union-subclass, it may change the root entitybinding in case of root entity is abstract
		// so the root table is not physical
		applyToAllEntityHierarchies( bindIdentifierGeneratorExecutor() );

		// Resolve associations:
		// - determine if JPA @OneToOne translates to Hibernate's one-to-one or unique many-to-one;
		// - determine if JPA @OneToMany translates to Hibernate's one-to-many or unique many-to-many.
		applyToAllEntityHierarchies( resolveAssociationSourcesExecutor() );

		// At this point, SourceIndex has all necessary information.

		// Bind all composite attribute containers. This includes composite sub-attributes.
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( false, SingularAttributeNature.COMPOSITE ) );

		// bind basic singular attributes, including composite sub-attributes that are basic.
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( false, SingularAttributeNature.BASIC ) );

		// many-to-one needs to be bound before one-to-one (um, can't remember why).

		// bind non-mappedby many-to-one and one-to-one attributes, including composite sub-attributes that are many-to-one/one-to-one.
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( false, SingularAttributeNature.MANY_TO_ONE ) );
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( false, SingularAttributeNature.ONE_TO_ONE ) );

		// bind mappedby many-to-one and one-to-one attributes, including composite sub-attributes that are many-to-one/one-to-one.
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( true, SingularAttributeNature.MANY_TO_ONE ) );
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( true, SingularAttributeNature.ONE_TO_ONE ) );

		// bind plural attributes (non-mappedBy first), including composite sub-attributes that are plural
		applyToAllEntityHierarchies( bindPluralAttributesExecutor( false ) );
		applyToAllEntityHierarchies( bindPluralAttributesExecutor( true ) );

		// Bind constraints after all attributes have been bound and the
		// columns used by attributes is already determined.
		applyToAllEntityHierarchies( bindConstraintsExecutor() );

		// TODO: check if any many-to-one attribute bindings with logicalOneToOne == false have all columns
		//       (and no formulas) contained in a defined unique key that only contains these columns.
		//       if so, mark the many-to-one as a logical one-to-one.
		// TODO: when does this have to be done.

		eventBus.finishUpAttributes();
	}

	private BinderStepHierarchyStrategy bindIdentifierGeneratorExecutor() {
		return new BinderStepHierarchyStrategy() {
			@Override
			public void visit(EntityHierarchySource source, BinderLocalBindingContext context) {
				bindIdentifierGenerator( context.locateBinding( source ).getRootEntityBinding() );
			}
		};
	}

	private void applyToAllEntityHierarchies(BinderStepHierarchyStrategy strategy) {
		processHelper.apply( sourceIndex.getAllHierarchySources(), strategy );
	}

	private void applyToAllEntityHierarchies(BinderStepEntityStrategy strategy) {
		processHelper.apply( sourceIndex.getAllHierarchySources(), strategy );
	}

	private void applyToAllEntityHierarchies(BinderStepCombinedStrategy strategy) {
		processHelper.apply( sourceIndex.getAllHierarchySources(), strategy );
	}

	private void applyToAllEntityHierarchies(
			BinderStepHierarchyStrategy hierarchyStrategy,
			BinderStepEntityStrategy entityStrategy) {
		processHelper.apply( sourceIndex.getAllHierarchySources(), hierarchyStrategy, entityStrategy );
	}

	private BinderStepEntityStrategy resolveAssociationSourcesExecutor() {
		return new BinderStepEntityStrategy() {
			@Override
			public boolean applyToRootEntity() {
				return true;
			}

			@Override
			public void visit(EntitySource source, BinderLocalBindingContext context) {
				sourceIndex.resolveAssociationSources( source, context );
			}

			@Override
			public void afterAllEntitiesInHierarchy() {
			}
		};
	}








	public BinderLocalBindingContext localBindingContext() {
		return rootBindingContext.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext();
	}

	public HibernateTypeHelper typeHelper() {
		return rootBindingContext.typeHelper();
	}

	public RelationalIdentifierHelper relationalIdentifierHelper() {
		return rootBindingContext.relationalIdentifierHelper();
	}

	public TableHelper tableHelper() {
		return rootBindingContext.tableHelper();
	}

	public ForeignKeyHelper foreignKeyHelper() {
		return rootBindingContext.foreignKeyHelper();
	}

	public RelationalValueBindingHelper relationalValueBindingHelper() {
		return rootBindingContext.relationalValueBindingHelper();
	}

	public NaturalIdUniqueKeyHelper naturalIdUniqueKeyHelper() {
		return rootBindingContext.naturalIdUniqueKeyHelper();
	}

	private void bindEntityHierarchiesExcludingNonIdAttributeBindings() {
		// unlike most of the other "steps" in the Binder, here we create an
		// instance per hierarchy; the reason being that it is easier to fire
		// notifications of the identifier being completely bound this way.
		for ( final EntityHierarchySource entityHierarchySource : sourceIndex.getAllHierarchySources() ) {
			final BinderStepCombinedStrategy strategy = new IdentifierAndSecondaryTableBindingStep( entityHierarchySource );
			processHelper.apply( entityHierarchySource, strategy );
		}

		eventBus.finishUpIdentifiers();
	}

	public static AttributeBindingContainer locateAttributeBindingContainer(
			EntityBinding entityBinding,
			String containerPath,
			BinderRootContext context) {
		if ( StringHelper.isEmpty( containerPath ) ) {
			return entityBinding;
		}

		final AttributeBinding attributeBinding = entityBinding.locateAttributeBindingByPath( containerPath, false );
		if ( AttributeBindingContainer.class.isInstance( attributeBinding ) ) {
			return (AttributeBindingContainer) attributeBinding;
		}
		else if ( EmbeddedAttributeBinding.class.isInstance( attributeBinding ) ) {
			return ( (EmbeddedAttributeBinding) attributeBinding ).getEmbeddableBinding();
		}

		throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext().makeMappingException(
				"Could not determine how to treat resolved attribute binding [" + attributeBinding +
						"] as AttributeBindingContainer"
		);
	}

	private BinderStepEntityStrategy bindSingularAttributesExecutor(
			final boolean isMappedBy,
			final SingularAttributeNature singularAttributeNature) {
		return new BinderStepEntityStrategy() {
			@Override
			public boolean applyToRootEntity() {
				return true;
			}

			@Override
			public void visit(EntitySource source, BinderLocalBindingContext context) {
				final EntityBinding binding = context.locateBinding( source );
				bindSingularAttributes( binding, isMappedBy, singularAttributeNature );
			}

			@Override
			public void afterAllEntitiesInHierarchy() {
			}
		};
	}

	private Map<EmbeddableBinding,EmbeddedAttributeBinding> embeddableToContributingAttributeMap
			= new HashMap<EmbeddableBinding, EmbeddedAttributeBinding>();


	// TODO: create separate methods that are more clear for the cases.
	private void bindSingularAttributes(
			final EntityBinding entityBinding,
			final boolean isMappedBy,
			final SingularAttributeNature singularAttributeNature) {
		// Get the map of all attributes for the entity binding of the specified nature.
		Map<AttributeRole, SingularAttributeSource> map = sourceIndex.getSingularAttributeSources(
				entityBinding.getEntityName(),
				isMappedBy,
				singularAttributeNature
		);
		for ( SingularAttributeSource attributeSource : map.values() ) {
			final String containerPath = attributeSource.getAttributePath().getParent() == null
					? ""
					: attributeSource.getAttributePath().getParent().getFullPath();

			final AttributeBindingContainer attributeBindingContainer = locateAttributeBindingContainer(
					entityBinding,
					containerPath,
					rootBindingContext
			);
			if (  isMappedBy ) {
				if ( !ToOneAttributeSource.class.isInstance( attributeSource ) ) {
					throw new AssertionFailure(
							String.format(
									Locale.ENGLISH,
									"mappedBy is true, but attributeSource is not an association: %s",
									attributeSource.getAttributeRole().getFullPath()
							)
					);
				}
				bindMappedBySecondaryTableIfNecessary( entityBinding, (ToOneAttributeSource) attributeSource );
			}

			if ( singularAttributeNature == SingularAttributeNature.COMPOSITE ) {
				// This only creates the composite attribute container.
				EmbeddedAttributeBinding attributeBinding = createAggregatedCompositeAttribute(
						attributeBindingContainer,
						(EmbeddedAttributeSource) attributeSource,
						null
				);

				embeddableToContributingAttributeMap.put( attributeBinding.getEmbeddableBinding(), attributeBinding );

				Aggregate aggregate = (Aggregate) attributeBinding.getAttribute().getSingularAttributeType();

				typeHelper().bindAggregatedCompositeAttributeType(
						localBindingContext().getServiceRegistry(),
						false,
						aggregate,
						aggregate.getDescriptor(),
						attributeBinding
				);
			}
			else if ( attributeBindingContainer instanceof EmbeddableBinding ) {
				// This attribute source is within a composite; skip binding if it is the parent.
				final EmbeddableBinding embeddableBinding = (EmbeddableBinding) attributeBindingContainer;
				final SingularAttribute parentReference = embeddableBinding.getParentReference();

				if ( parentReference == null || !parentReference.getName().equals( attributeSource.getName() ) ) {
					bindAttribute( attributeBindingContainer, attributeSource );

					final EmbeddedAttributeBinding embeddedAttributeBinding
							= embeddableToContributingAttributeMap.get( embeddableBinding );
					if ( embeddedAttributeBinding == null ) {
						throw localBindingContext().makeMappingException(
								"Could not resolve embeddable binding back to the attribute that contributed it : " +
										attributeSource.getAttributeRole().getFullPath()
						);
					}

					final EmbeddedAttributeSource embeddedAttributeSource
							= (EmbeddedAttributeSource) sourceIndex.attributeSource( embeddedAttributeBinding.getAttributeRole() );
					if ( embeddedAttributeSource == null ) {
						throw localBindingContext().makeMappingException(
								"Could not resolve embeddable binding back to the attribute source that contributed it : " +
										attributeSource.getAttributeRole().getFullPath()
						);
					}

					completeCompositeAttributeBindingIfPossible(
							embeddedAttributeBinding,
							embeddedAttributeSource
					);
				}
			}
			else {
				// The container is the EntityBinding itself.
				bindAttribute( attributeBindingContainer, attributeSource );
			}
		}
	}

	// All sub-attributes must be bound before it's type and ComponentMetamodel can be determined.
	private void completeCompositeAttributeBindingIfPossible(
			EmbeddedAttributeBinding embeddedAttributeBinding,
			EmbeddedAttributeSource compositeAttributeSource) {
		// Find out the number of sub-attributes, excluding the parent attribute.
		final int nAttributeSourcesExcludingParent = embeddedAttributeBinding.getEmbeddableBinding().getParentReference() != null
				? compositeAttributeSource.getEmbeddableSource().attributeSources().size() - 1
				: compositeAttributeSource.getEmbeddableSource().attributeSources().size();
		if ( embeddedAttributeBinding.getEmbeddableBinding().attributeBindingSpan() == nAttributeSourcesExcludingParent ) {
			// All sub-attribute bindings are present; now check if all sub-attributes have
			// their type resolved.
			boolean allResolved = true;
			for ( AttributeBinding attributeBinding : embeddedAttributeBinding.getEmbeddableBinding().attributeBindings() ) {
				if ( attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() == null ) {
					// Something is not resolved.
					allResolved = false;
					break;
				}
			}
			if ( allResolved ) {
				final Aggregate aggregate = (Aggregate) embeddedAttributeBinding.getAttribute().getSingularAttributeType();

				// All are resolved, so we can bind the type.
				typeHelper().bindAggregatedCompositeAttributeType(
						localBindingContext().getServiceRegistry(),
						false,
						aggregate,
						aggregate.getDescriptor(),
						embeddedAttributeBinding
				);
				// Now check the container.
				if ( embeddedAttributeBinding.getContainer() instanceof EmbeddableBinding ) {
					// The container is also a EmbeddableBinding.
					// We need this process for the container.
					final EmbeddedAttributeBinding parentEmbeddedAttributeBinding = embeddableToContributingAttributeMap.get(
							embeddedAttributeBinding.getContainer()
					);
					final EmbeddedAttributeSource parentCompositeAttributeSource
							= (EmbeddedAttributeSource) sourceIndex.attributeSource( parentEmbeddedAttributeBinding.getAttributeRole() );

					completeCompositeAttributeBindingIfPossible(
							parentEmbeddedAttributeBinding,
							parentCompositeAttributeSource
					);
				}
			}
		}
	}

	private BinderStepEntityStrategy bindPluralAttributesExecutor(final boolean isInverse) {
		 return new BinderStepEntityStrategy() {
			 @Override
			 public boolean applyToRootEntity() {
				 return true;
			 }

			 @Override
			public void visit(EntitySource source, BinderLocalBindingContext context) {
				// TODO: may want bind plural attributes of a particular element nature.
				final EntityBinding entityBinding = context.locateBinding( source );
				// Get the map for inverse or non-inverse (as specified) plural attributes
				Map<AttributeRole, PluralAttributeSource> map = sourceIndex.getPluralAttributeSources(
						entityBinding.getEntityName(),
						isInverse
				);
				for ( PluralAttributeSource attributeSource : map.values() ) {
					// Bind the attribute into the appropriate container.
					final String containerPath = attributeSource.getAttributePath().getParent() == null
							? ""
							: attributeSource.getAttributePath().getParent().getFullPath();
					final AttributeBindingContainer attributeBindingContainer =
							locateAttributeBindingContainer( entityBinding, containerPath, rootBindingContext );

					bindAttribute( attributeBindingContainer, attributeSource );

					if ( attributeBindingContainer instanceof EmbeddableBinding ) {
						// We just bound a sub-attribute into a EmbeddedAttributeBinding.
						final EmbeddableBinding embeddableBinding = (EmbeddableBinding) attributeBindingContainer;

						final EmbeddedAttributeBinding embeddedAttributeBinding = embeddableToContributingAttributeMap.get(
								embeddableBinding
						);

						final EmbeddedAttributeSource compositeAttributeSource = (EmbeddedAttributeSource) sourceIndex.attributeSource(
								embeddedAttributeBinding.getAttributeRole()
						);

						// Resolve the type if types are resolved for all sub-attributes now.
						completeCompositeAttributeBindingIfPossible( embeddedAttributeBinding, compositeAttributeSource );
					}
				}
			}

			 @Override
			 public void afterAllEntitiesInHierarchy() {
			 }
		 };
	}


	private BinderStepEntityStrategy bindConstraintsExecutor() {
		return new BinderStepEntityStrategy() {
			@Override
			public boolean applyToRootEntity() {
				return true;
			}

			@Override
			public void visit(EntitySource source, BinderLocalBindingContext context) {
				final EntityBinding entityBinding = context.locateBinding( source );
				for ( final ConstraintSource constraintSource : source.getConstraints() ) {
					final TableSpecification table = findConstraintTable( entityBinding, constraintSource.getTableName() );
					final List<Column> columns = new ArrayList<Column>();
					for ( final String columnName : constraintSource.columnNames() ) {
						final Column column = tableHelper().locateColumn( table, columnName,
								new ColumnNamingStrategyHelper( null, false ) );
						if (column == null) {
							throw new MappingException( "While creating a constraint, could not find column "
									+ columnName + " on table "+ table.getLogicalName().getText() );
						}
						columns.add( column );
					}

					if ( UniqueConstraintSource.class.isInstance( constraintSource ) ) {
						tableHelper().createUniqueKey( table, columns, constraintSource.name() );
					}
					else if ( IndexConstraintSource.class.isInstance( constraintSource ) ) {
						final IndexConstraintSource indexConstraintSource = (IndexConstraintSource) constraintSource;
						tableHelper().createIndex( table, columns, indexConstraintSource.name(),
								indexConstraintSource.isUnique() );
					}
				}
			}

			@Override
			public void afterAllEntitiesInHierarchy() {
			}
		};
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity binding related methods

	private static String determineReferencedEntityName(
			final JavaTypeDescriptor referencedEntityTypeDescriptor,
			final String explicitEntityName) {
		if ( explicitEntityName != null ) {
			return explicitEntityName;
		}
		else if ( referencedEntityTypeDescriptor != null ) {
			return referencedEntityTypeDescriptor.getName().toString();
		}
		else {
			throw new IllegalArgumentException( "explicitEntityName and entityJavaClassReference cannot both be null." );
		}
	}

	private EntityBinding locateEntityBinding(
			final JavaTypeDescriptor referencedEntityTypeDescriptor,
			final String explicitEntityName) {
		final String referencedEntityName = determineReferencedEntityName(
				referencedEntityTypeDescriptor,
				explicitEntityName
		);
		return locateEntityBinding( referencedEntityName );
	}

	private EntityBinding locateEntityBinding(final String entityName) {
		// Check if binding has already been created
		EntityBinding entityBinding = rootBindingContext.getMetadataCollector().getEntityBinding( entityName );
		if ( entityBinding == null ) {
			 throw localBindingContext().makeMappingException(
					 String.format( "No entity binding with name: %s", entityName )
			 );
		}
		return entityBinding;
	}



	private FilterConfiguration createFilterConfiguration(FilterSource filterSource, EntityBinding entityBinding){
		String condition = filterSource.getCondition();
		if(StringHelper.isEmpty( condition )){
			FilterDefinition filterDefinition = rootBindingContext.getMetadataCollector().getFilterDefinitions().get( filterSource.getName() );
			if(filterDefinition == null){
				throw localBindingContext().makeMappingException(
						String.format(
								"Filter[%s] doesn't have a condition",
								filterSource.getName()
						)
				);
			}
			condition = filterDefinition.getDefaultFilterCondition();
		}
		return new FilterConfiguration(
				filterSource.getName(),
				condition,
				filterSource.shouldAutoInjectAliases(),
				filterSource.getAliasToTableMap(),
				filterSource.getAliasToEntityMap(),
				entityBinding
		);
	}

	private void bindPrimaryTable(
			final EntityBinding entityBinding,
			final EntitySource entitySource) {
		final EntityBinding superEntityBinding = entityBinding.getSuperEntityBinding();
		final InheritanceType inheritanceType = entityBinding.getHierarchyDetails().getInheritanceType();
		final TableSpecification table;
		final String tableName;
		if ( superEntityBinding != null && inheritanceType == InheritanceType.SINGLE_TABLE ) {
			// single table and sub entity
			table = superEntityBinding.getPrimaryTable();
			tableName = superEntityBinding.getPrimaryTableName();
		}
		else {
			// single table and root entity
			// joined
			// table per class and non-abstract  entity
			Table includedTable = null;
			if ( superEntityBinding != null
					&& inheritanceType == InheritanceType.TABLE_PER_CLASS
					&& Table.class.isInstance( superEntityBinding.getPrimaryTable() ) ) {
				includedTable = Table.class.cast( superEntityBinding.getPrimaryTable() );
			}
			table = tableHelper().createTable(
					entitySource.getPrimaryTable(), new TableNamingStrategyHelper( entityBinding ), includedTable
			);
			tableName = table.getLogicalName().getText();
		}

		entityBinding.setPrimaryTable( table );
		entityBinding.setPrimaryTableName( tableName );
	}

	private void bindMappedBySecondaryTableIfNecessary(EntityBinding entityBinding, ToOneAttributeSource attributeSource) {
		if ( ! attributeSource.isMappedBy() ) {
			throw new IllegalArgumentException( "attributeSource does not have mappedBy defined." );
		}
		final MappedByAssociationSource mappedByAssociationSource = (MappedByAssociationSource) attributeSource;

		final ToOneAttributeSource ownerAttributeSource = (ToOneAttributeSource) sourceIndex.attributeSource(
				mappedByAssociationSource.getReferencedEntityName(),
				mappedByAssociationSource.getMappedBy()
		);
		if ( ownerAttributeSource.isMappedBy() ) {
			throw new AssertionFailure( "owner attribute source has mappedBy != null" );
		}

		final EntityBinding ownerEntityBinding = locateEntityBinding( attributeSource.getReferencedEntityName() );

		if ( ownerAttributeSource.getSingularAttributeNature() == SingularAttributeNature.ONE_TO_ONE ||
				ownerAttributeSource.getContainingTableName() == null ||
				ownerAttributeSource.getContainingTableName().equals( ownerEntityBinding.getPrimaryTableName() ) ) {
			// primary table is used, so no need to bind an inverse secondary table.
			return;
		}

		final AttributeBinding ownerAttributeBinding =
				ownerEntityBinding.locateAttributeBindingByPath( mappedByAssociationSource.getMappedBy(), true );
		if ( !SingularAssociationAttributeBinding.class.isInstance( ownerAttributeBinding ) ) {
			throw new AssertionFailure( "Owner is not a one-to-one or many-to-one association." );
		}
		final SingularAssociationAttributeBinding ownerAssociationAttributeBinding =
				(SingularAssociationAttributeBinding) ownerAttributeBinding;

		// If the owner side of the association uses a secondary table, then an inverse secondary table needs
		// to be bound for the entity binding containing the mappedBy attribute..
		final TableSpecification ownerTable = ownerAssociationAttributeBinding.getTable();
		if ( !ownerEntityBinding.getPrimaryTable().equals( ownerTable ) ) {
			final SecondaryTable ownerSecondaryTable =
					ownerEntityBinding.getSecondaryTables().get(
							ownerTable.getLogicalName()
					);
		if ( ownerSecondaryTable == null ) {
				throw new AssertionFailure( "Owner association does not use primary table, but no secondary table was found." );
			}
		}

		if ( entityBinding.getPrimaryTable().equals( ownerTable ) ||
				entityBinding.getSecondaryTables().containsKey( ownerTable.getLogicalName() ) ) {
			// table already exists; nothing to do.
			return;
		}

		// locate the foreign key
		final List<Column> targetColumns = new ArrayList<Column>( ownerAssociationAttributeBinding.getRelationalValueBindings().size() );
		for ( Value targetValue : ownerAssociationAttributeBinding.getReferencedAttributeBinding().getValues() ) {
			targetColumns.add( (Column) targetValue );
		}
		final ForeignKey foreignKey = foreignKeyHelper().locateOrCreateForeignKey(
				null,
				ownerTable,
				extractColumnsFromRelationalValueBindings( ownerAssociationAttributeBinding.getRelationalValueBindings() ),
				entityBinding.getPrimaryTable(),
				targetColumns,
				attributeSource.isCascadeDeleteEnabled(),
				true
		);
		if ( foreignKey == null ) {
			throw new AssertionFailure( "Foreign key not found; should have been defined by owner side of association." );
		}
		final SecondaryTable ownerSecondaryTable = ownerEntityBinding.getSecondaryTables().get( ownerTable.getLogicalName() );
		SecondaryTable secondaryTable = new SecondaryTable( ownerTable, foreignKey );
		secondaryTable.setInverse( true );
		secondaryTable.setOptional( ownerSecondaryTable.isOptional() );
		// TODO: how should this be set???
		secondaryTable.setCascadeDeleteEnabled( false );
		secondaryTable.setCustomDelete( ownerSecondaryTable.getCustomDelete() );
		secondaryTable.setCustomInsert( ownerSecondaryTable.getCustomInsert() );
		secondaryTable.setCustomUpdate( ownerSecondaryTable.getCustomUpdate() );
		entityBinding.addSecondaryTable( secondaryTable );
		rootBindingContext.getMetadataCollector().addSecondaryTable( secondaryTable );
	}



	private List<RelationalValueBinding> getJoinedPrimaryKeyRelationalValueBindings(
			EntityBinding entityBinding,
			List<ColumnSource> joinedPrimaryKeyColumnSources,
			TableSpecification table) {
		final List<Column> primaryKeyColumns = entityBinding.getPrimaryTable().getPrimaryKey().getColumns();

		final List<RelationalValueBinding> joinRelationalValueBindings;
		// TODO: deal with property-refs???
		if ( joinedPrimaryKeyColumnSources.isEmpty() ) {
			joinRelationalValueBindings = new ArrayList<RelationalValueBinding>( primaryKeyColumns.size() );
			for ( Column joinedColumn : primaryKeyColumns ) {
				Column joinColumn = table.locateOrCreateColumn(
						localBindingContext().getBuildingOptions().getNamingStrategy().joinKeyColumnName(
								joinedColumn.getColumnName().getText(),
								entityBinding.getPrimaryTable().getLogicalName().getText()
						)
				);
				joinColumn.setNullable( false );
				table.getPrimaryKey().addColumn( joinColumn );
				joinRelationalValueBindings.add( new RelationalValueBinding( entityBinding.getPrimaryTable(), joinColumn, true, false ) );
			}
		}
		else {
			joinRelationalValueBindings = CollectionHelper.arrayList( joinedPrimaryKeyColumnSources.size() );
			if ( primaryKeyColumns.size() != joinedPrimaryKeyColumnSources.size() ) {
				throw localBindingContext().makeMappingException(
						String.format(
								"The number of primary key column sources provided for a secondary table is not equal to the number of columns in the primary key for [%s].",
								entityBinding.getEntityName()
						)
				);
			}
			for ( int i = 0; i < primaryKeyColumns.size(); i++ ) {
				// todo : apply naming strategy to infer missing column name
				final ColumnSource joinColumnSource = joinedPrimaryKeyColumnSources.get( i );
				Column column = table.locateColumn( joinColumnSource.getName() );
				if ( column == null ) {
					column = tableHelper().locateOrCreateColumn(
							table,
							joinColumnSource,
							new ColumnNamingStrategyHelper(
									null,
									false
							),
							true,
							false
					);
					table.getPrimaryKey().addColumn( column );
				}
				joinRelationalValueBindings.add( new RelationalValueBinding( table, column, true, false ) );
			}
		}

		final EntityIdentifier idInfo = entityBinding.getHierarchyDetails().getEntityIdentifier();

		final Type idType;
		if ( idInfo.getNature() == EntityIdentifierNature.NON_AGGREGATED_COMPOSITE ) {
			final NonAggregatedCompositeIdentifierBinding idBinding =
					(NonAggregatedCompositeIdentifierBinding) idInfo.getEntityIdentifierBinding();
			idType = idBinding.getHibernateType(
					localBindingContext().getServiceRegistry(),
					localBindingContext().getMetadataCollector().getTypeResolver().getTypeFactory()
			);
		}
		else {
			final EntityIdentifier.AttributeBasedIdentifierBinding idBinding =
					(EntityIdentifier.AttributeBasedIdentifierBinding) idInfo.getEntityIdentifierBinding();
			idType = idBinding.getAttributeBinding().getHibernateTypeDescriptor().getResolvedTypeMapping();
		}

		typeHelper().bindJdbcDataType( idType, joinRelationalValueBindings );
		return joinRelationalValueBindings;
	}

	public ForeignKey locateOrCreateForeignKey(
			final String foreignKeyName,
			final TableSpecification sourceTable,
			final List<RelationalValueBinding> sourceRelationalValueBindings,
			final TableSpecification targetTable,
			final List<Column> targetColumns,
			boolean isCascadeDeleteEnabled,
			boolean createConstraint) {
		return foreignKeyHelper().locateOrCreateForeignKey(
				foreignKeyName,
				sourceTable,
				extractColumnsFromRelationalValueBindings( sourceRelationalValueBindings ),
				targetTable,
				targetColumns,
				isCascadeDeleteEnabled,
				createConstraint
		);
	}

	// TODO: try to get rid of this...
	private static List<Column> extractColumnsFromRelationalValueBindings(
			final List<RelationalValueBinding> valueBindings) {
		List<Column> columns = new ArrayList<Column>( valueBindings.size() );
		for ( RelationalValueBinding relationalValueBinding : valueBindings ) {
			final Value value = relationalValueBinding.getValue();
			// todo : currently formulas are not supported here... :(
			if ( !Column.class.isInstance( value ) ) {
				throw new NotYetImplementedException(
						"Derived values are not supported when creating a foreign key that targets columns."
				);
			}
			columns.add( (Column) value );
		}
		return columns;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ identifier binding relates methods



	private void bindIdentifierGenerator(final EntityBinding rootEntityBinding) {
		final Properties properties = new Properties();
		properties.putAll(
				rootBindingContext.getBuildingOptions()
						.getServiceRegistry()
						.getService( ConfigurationService.class )
						.getSettings()
		);
		if ( !properties.contains( AvailableSettings.PREFER_POOLED_VALUES_LO ) ) {
			properties.put( AvailableSettings.PREFER_POOLED_VALUES_LO, "false" );
		}
		if ( !properties.contains( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER ) ) {
			properties.put(
					PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
					rootBindingContext.getMetadataCollector().getObjectNameNormalizer()
			);
		}
		final EntityIdentifier entityIdentifier = rootEntityBinding.getHierarchyDetails().getEntityIdentifier();
		entityIdentifier.createIdentifierGenerator(
				rootBindingContext.getMetadataCollector().getIdentifierGeneratorFactory(),
				properties
		);
		if ( IdentityGenerator.class.isInstance( entityIdentifier.getIdentifierGenerator() ) ) {
			if ( rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan() != 1 ) {
				throw localBindingContext().makeMappingException(
						String.format(
								"ID for %s is mapped as an identity with %d columns. IDs mapped as an identity can only have 1 column.",
								rootEntityBinding.getEntityName(),
								rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan()
						)
				);
			}
			rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 ).setIdentity( true );
		}
		if ( PersistentIdentifierGenerator.class.isInstance( entityIdentifier.getIdentifierGenerator() ) ) {
			( (PersistentIdentifierGenerator) entityIdentifier.getIdentifierGenerator() ).registerExportables(
					rootBindingContext.getMetadataCollector().getDatabase()
			);
		}
	}

	private SingularAttributeBinding bindIdentifierAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource) {
		return bindSingularAttribute( attributeBindingContainer, attributeSource, true );
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Attributes binding relates methods
	private void bindAttributes(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSourceContainer attributeSourceContainer) {
		for ( final AttributeSource attributeSource : attributeSourceContainer.attributeSources() ) {
			bindAttribute( attributeBindingContainer, attributeSource );
		}
	}

	private void bindAttributes(
			final EmbeddableBinding embeddableBinding,
			final AttributeSourceContainer attributeSourceContainer) {
		if ( embeddableBinding.getParentReference() == null ) {
			bindAttributes(
					(AttributeBindingContainer) embeddableBinding,
					attributeSourceContainer
			);
		}
		else {
			for ( final AttributeSource subAttributeSource : attributeSourceContainer.attributeSources() ) {
				if ( !subAttributeSource.getName().equals( embeddableBinding.getParentReference().getName() ) ) {
					bindAttribute(
							embeddableBinding,
							subAttributeSource
					);
				}
			}
		}

	}

	private AttributeBinding bindAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSource attributeSource) {
		// Return existing binding if available
		final String attributeName = attributeSource.getName();
		final AttributeBinding attributeBinding = attributeBindingContainer.locateAttributeBinding( attributeName );
		if ( attributeBinding != null ) {
			return attributeBinding;
		}
		if ( attributeSource.isSingular() ) {
			return bindSingularAttribute(
					attributeBindingContainer,
					SingularAttributeSource.class.cast( attributeSource ),
					false
			);
		}
		else {
			return bindPluralAttribute(
					attributeBindingContainer,
					PluralAttributeSource.class.cast( attributeSource )
			);
		}
	}

	private BasicAttributeBinding bindBasicAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource,
			SingularAttribute attribute) {
		if ( attribute == null ) {
			attribute = createSingularAttribute( attributeBindingContainer, attributeSource );
		}
		final List<RelationalValueBinding> relationalValueBindings = relationalValueBindingHelper().createRelationalValueBindings(
				attributeBindingContainer,
				attributeSource,
				attribute,
				locateDefaultTableSpecificationForAttribute( attributeBindingContainer, attributeSource ),
				false
		);
		final BasicAttributeBinding attributeBinding = attributeBindingContainer.makeBasicAttributeBinding(
				attribute,
				relationalValueBindings,
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				attributeSource.isLazy(),
				attributeSource.getNaturalIdMutability(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath(),
				attributeSource.getGeneration()
		);
		typeHelper().bindSingularAttributeType(
				attributeSource,
				attributeBinding
		);
		return attributeBinding;
	}


	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ singular attributes binding
	private SingularAttributeBinding bindSingularAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource,
			final boolean isIdentifierAttribute) {
		final SingularAttributeNature singularAttributeNature = attributeSource.getSingularAttributeNature();
		final SingularAttribute attribute =
				attributeBindingContainer.getAttributeContainer().locateSingularAttribute( attributeSource.getName() );
		switch ( singularAttributeNature ) {
			case BASIC:
				return bindBasicAttribute( attributeBindingContainer, attributeSource, attribute );
			case ONE_TO_ONE:
				return bindOneToOneAttribute(
						attributeBindingContainer,
						ToOneAttributeSource.class.cast( attributeSource ),
						attribute
				);
			case MANY_TO_ONE:
				return bindManyToOneAttribute(
						attributeBindingContainer,
						ToOneAttributeSource.class.cast( attributeSource ),
						attribute
				);
			case COMPOSITE:
				return bindAggregatedCompositeAttribute(
						attributeBindingContainer,
						EmbeddedAttributeSource.class.cast( attributeSource ),
						attribute,
						isIdentifierAttribute
				);
			default:
				throw new NotYetImplementedException( singularAttributeNature.toString() );
		}
	}

	private EmbeddedAttributeBinding bindAggregatedCompositeAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final EmbeddedAttributeSource attributeSource,
			SingularAttribute attribute,
			boolean isAttributeIdentifier) {
		EmbeddedAttributeBinding attributeBinding = createAggregatedCompositeAttribute(
				attributeBindingContainer, attributeSource, attribute
		);
		bindAttributes( attributeBinding.getEmbeddableBinding(), attributeSource.getEmbeddableSource() );
		typeHelper().bindAggregatedCompositeAttributeType(
				localBindingContext().getServiceRegistry(),
				isAttributeIdentifier,
				(Aggregate) attributeBinding.getAttribute().getSingularAttributeType(),
				null, // TODO: don't have the default value at this point; shouldn't be needed...
				attributeBinding
		);
		return attributeBinding;
	}

	private EmbeddedAttributeBinding createAggregatedCompositeAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final EmbeddedAttributeSource attributeSource,
			SingularAttribute attribute) {
		final Aggregate composite;
		if ( attribute == null ) {
			// see if the AttributeContainer already has an attribute for this name...
			attribute = attributeBindingContainer.getAttributeContainer().locateCompositeAttribute(
					attributeSource.getName()
			);
			if ( attribute != null ) {
				// validate its ok to use...
				if ( !Aggregate.class.isInstance( attribute.getSingularAttributeType() ) ) {
					throw localBindingContext().makeMappingException(
							"Found existing attribute on container for '" + attributeSource.getName()
									+ "', but was expecting an Aggregate"
					);
				}
				composite = (Aggregate) attribute.getSingularAttributeType();
			}
			else {
				final JavaTypeDescriptor compositeTypeDescriptor;
				if ( attributeSource.getEmbeddableSource().getTypeDescriptor() != null ) {
					compositeTypeDescriptor = attributeSource.getEmbeddableSource().getTypeDescriptor();
				}
				else {
					final EntityMode entityMode =
							attributeBindingContainer.seekEntityBinding().getHierarchyDetails().getEntityMode();
					compositeTypeDescriptor = typeHelper().determineJavaType(
							attributeSource,
							attributeBindingContainer.getAttributeContainer(),
							entityMode
					);
				}
				composite = new Aggregate( compositeTypeDescriptor, null );
				attribute = attributeBindingContainer.getAttributeContainer().createCompositeAttribute(
						attributeSource.getName(),
						composite
				);
			}
		}
		else {
			composite = (Aggregate) attribute.getSingularAttributeType();
		}

		final SingularAttribute referencingAttribute = StringHelper.isEmpty( attributeSource.getEmbeddableSource().getParentReferenceAttributeName() )
				? null
				: composite.createSingularAttribute( attributeSource.getEmbeddableSource().getParentReferenceAttributeName() );
		final NaturalIdMutability naturalIdMutability = attributeSource.getNaturalIdMutability();

		Class<? extends ComponentTuplizer> tuplizerClass = null;
		if ( attributeSource.getEmbeddableSource().getExplicitTuplizerClassName() != null ) {
			tuplizerClass = localBindingContext().getBuildingOptions().getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( attributeSource.getEmbeddableSource().getExplicitTuplizerClassName() );
		}
		final EmbeddedAttributeBinding attributeBinding = attributeBindingContainer.makeAggregatedCompositeAttributeBinding(
				attribute,
				referencingAttribute,
				tuplizerClass,
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				attributeSource.isLazy(),
				naturalIdMutability,
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath()
		);

		return attributeBinding;
	}

	private ManyToOneAttributeBinding bindManyToOneAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ToOneAttributeSource attributeSource,
			SingularAttribute attribute) {
		if ( attribute == null ) {
			attribute = createSingularAttribute( attributeBindingContainer, attributeSource );
		}

		final JavaTypeDescriptor referencedEntityTypeDescriptor = typeHelper().determineJavaType(
				attributeSource,
				attributeBindingContainer.getAttributeContainer()
		);
		final EntityBinding referencedEntityBinding = locateEntityBinding(
				referencedEntityTypeDescriptor,
				attributeSource.getReferencedEntityName()
		);

		final AssociationRelationalBindingResolver resolver = getAssociationRelationalBindingResolver( attributeSource );

		//now find the referenced attribute binding, either the referenced entity's id attribute or the referenced attribute
		//todo referenced entityBinding null check?
		final SingularAttributeBinding referencedAttributeBinding = resolver.resolveManyToOneReferencedAttributeBinding(
				attributeBindingContainer,
				attributeSource,
				referencedEntityBinding
		);

		// todo : currently a chicken-egg problem here between creating the attribute binding and binding its FK values...
		// now we have everything to create the attribute binding
		final ManyToOneAttributeBinding attributeBinding = attributeBindingContainer.makeManyToOneAttributeBinding(
				attribute,
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				attributeSource.isLazy(),
				attributeSource.isIgnoreNotFound(),
				attributeSource.getNaturalIdMutability(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath(),
				referencedEntityBinding,
				referencedAttributeBinding
		);

		/**
		 * this is not correct, here, if no @JoinColumn defined, we simply create the FK column only with column calucated
		 * but what we should do is get all the column info from the referenced column(s), including nullable, size etc.
		 */
		final List<RelationalValueBinding> relationalValueBindings = resolver.resolveManyToOneRelationalValueBindings(
				attributeSource,
				attributeBindingContainer,
				referencedAttributeBinding,
				referencedEntityBinding
		);
		final ForeignKey foreignKey =
				relationalValueBindings.isEmpty() || relationalValueBindingHelper().hasDerivedValue( relationalValueBindings ) ?
						null :
						resolver.resolveManyToOneForeignKey(
							attributeSource,
							attributeBindingContainer,
							relationalValueBindings,
							referencedEntityBinding
		);
		attributeBinding.setJoinRelationalValueBindings( relationalValueBindings, foreignKey );

		// Type resolution...
		if ( !attribute.isTypeResolved() ) {
			attribute.resolveType( referencedEntityBinding.getEntity() );
		}

		String uniqueKeyAttributeName = null;
		final EntityIdentifier idInfo = referencedEntityBinding.getHierarchyDetails().getEntityIdentifier();
		if ( idInfo.getNature() != EntityIdentifierNature.NON_AGGREGATED_COMPOSITE ) {
			final EntityIdentifier.AttributeBasedIdentifierBinding idBinding =
					(EntityIdentifier.AttributeBasedIdentifierBinding) idInfo.getEntityIdentifierBinding();
			final SingularAttributeBinding idAttrBinding = idBinding.getAttributeBinding();
			if ( referencedAttributeBinding != idAttrBinding ) {
				referencedAttributeBinding.setAlternateUniqueKey( true );
				uniqueKeyAttributeName = referencedAttributeBinding.getAttributePath().getFullPath();
			}
		}

		final Type resolvedType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().manyToOne(
				referencedEntityBinding.getEntityName(),
				uniqueKeyAttributeName == null,
				uniqueKeyAttributeName,
				attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
				attributeSource.isUnWrapProxy(),
				attributeSource.isIgnoreNotFound(),
				attributeSource.isUnique()
		);
		typeHelper().bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				attributeSource.getTypeInformation(),
				referencedEntityTypeDescriptor,
				resolvedType
		);

		typeHelper().bindJdbcDataType(
				attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				attributeBinding.getRelationalValueBindings()
		);

		bindToOneDetails( attributeSource, attributeBinding );

		return attributeBinding;
	}

	private OneToOneAttributeBinding bindOneToOneAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ToOneAttributeSource attributeSource,
			SingularAttribute attribute) {
		if ( attribute == null ) {
			attribute = createSingularAttribute( attributeBindingContainer, attributeSource );
		}
		final JavaTypeDescriptor referencedEntityJavaClassReference = typeHelper().determineJavaType(
				attributeSource,
				attributeBindingContainer.getAttributeContainer()
		);
		final EntityBinding referencedEntityBinding = locateEntityBinding(
				referencedEntityJavaClassReference,
				attributeSource.getReferencedEntityName()
		);

		final AssociationRelationalBindingResolver resolver = getAssociationRelationalBindingResolver( attributeSource );

		//now find the referenced attribute binding, either the referenced entity's id attribute or the referenced attribute
		//todo referenced entityBinding null check?
		final SingularAttributeBinding referencedAttributeBinding = resolver.resolveOneToOneReferencedAttributeBinding(
				attributeSource,
				referencedEntityBinding
		);
		// todo : currently a chicken-egg problem here between creating the attribute binding and binding its FK values...
		// now we have everything to create the attribute binding
		final OneToOneAttributeBinding attributeBinding = attributeBindingContainer.makeOneToOneAttributeBinding(
				attribute,
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				attributeSource.isLazy(),
				attributeSource.getNaturalIdMutability(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath(),
				referencedEntityBinding,
				referencedAttributeBinding,
				attributeSource.getForeignKeyDirection() == ForeignKeyDirection.FROM_PARENT
		);

		/**
		 * this is not correct, here, if no @JoinColumn defined, we simply create the FK column only with column calucated
		 * but what we should do is get all the column info from the referenced column(s), including nullable, size etc.
		 */
		final List<RelationalValueBinding> relationalValueBindings = resolver.resolveOneToOneRelationalValueBindings(
				attributeSource,
				attributeBindingContainer,
				referencedAttributeBinding
		);
		final ForeignKey foreignKey;
		if ( attributeSource.getForeignKeyDirection() == ForeignKeyDirection.FROM_PARENT ) {
			foreignKey = resolver.resolveOneToOneForeignKey(
					attributeSource,
					attributeBinding.getContainer().getPrimaryTable(),
					attributeBinding.getContainer().getPrimaryTable().getPrimaryKey().getColumns(),
					referencedEntityBinding
			);
		}
		else {
			foreignKey = null;
		}
		attributeBinding.setJoinRelationalValueBindings( relationalValueBindings, foreignKey );

		// Type resolution...
		if ( !attribute.isTypeResolved() ) {
			attribute.resolveType( referencedEntityBinding.getEntity() );
		}


		String uniqueKeyAttributeName = null;
		final EntityIdentifier idInfo = referencedEntityBinding.getHierarchyDetails().getEntityIdentifier();
		if ( idInfo.getNature() != EntityIdentifierNature.NON_AGGREGATED_COMPOSITE ) {
			final EntityIdentifier.AttributeBasedIdentifierBinding idBinding =
					(EntityIdentifier.AttributeBasedIdentifierBinding) idInfo.getEntityIdentifierBinding();
			final SingularAttributeBinding idAttrBinding = idBinding.getAttributeBinding();
			if ( referencedAttributeBinding != idAttrBinding ) {
				referencedAttributeBinding.setAlternateUniqueKey( true );
				uniqueKeyAttributeName = referencedAttributeBinding.getAttributePath().getFullPath();
			}
		}

		final Type resolvedType;
		if ( attributeSource.isMappedBy() || attributeSource.relationalValueSources().isEmpty() )  {
			resolvedType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().oneToOne(
					referencedEntityBinding.getEntityName(),
					attributeSource.getForeignKeyDirection(),
					uniqueKeyAttributeName == null,
					uniqueKeyAttributeName,
					attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
					attributeSource.isUnWrapProxy(),
					attributeBindingContainer.seekEntityBinding().getEntityName(),
					attribute.getName()
			);
		}
		else {
			resolvedType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().specialOneToOne(
					referencedEntityBinding.getEntityName(),
					attributeSource.getForeignKeyDirection(),
					uniqueKeyAttributeName == null,
					uniqueKeyAttributeName,
					attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
					attributeSource.isUnWrapProxy(),
					attributeBindingContainer.seekEntityBinding().getEntityName(),
					attribute.getName()
			);
		}

		typeHelper().bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				attributeSource.getTypeInformation(),
				referencedEntityJavaClassReference,
				resolvedType
		);
		if ( !attributeBinding.getRelationalValueBindings().isEmpty() ) {
			typeHelper().bindJdbcDataType(
					attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
					attributeBinding.getRelationalValueBindings()
			);
		}
		bindToOneDetails( attributeSource, attributeBinding );

		return attributeBinding;
	}

	private void bindToOneDetails(
			final ToOneAttributeSource attributeSource,
			final SingularAssociationAttributeBinding attributeBinding) {
		attributeBinding.setCascadeStyle( determineCascadeStyle( attributeSource.getCascadeStyles() ) );
		attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
		attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ plural attributes binding
	private AbstractPluralAttributeBinding bindPluralAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource) {
		final PluralAttributeNature nature = attributeSource.getNature();
		final PluralAttribute attribute =
				attributeBindingContainer.getAttributeContainer().locatePluralAttribute( attributeSource.getName() );
		final AbstractPluralAttributeBinding attributeBinding;
		switch ( nature ) {
			case BAG:
				attributeBinding = bindBagAttribute( attributeBindingContainer, attributeSource, attribute );
				break;
			case SET:
				attributeBinding = bindSetAttribute( attributeBindingContainer, attributeSource, attribute );
				break;
			case LIST:
				attributeBinding = bindListAttribute(
						attributeBindingContainer,
						(IndexedPluralAttributeSource) attributeSource,
						attribute
				);
				break;
			case MAP:
				attributeBinding = bindMapAttribute(
						attributeBindingContainer,
						attributeSource,
						attribute
				);
				break;
			case ARRAY:
				attributeBinding = bindArrayAttribute(
						attributeBindingContainer,
						(IndexedPluralAttributeSource) attributeSource,
						attribute
				);
				break;
			default:
				throw new NotYetImplementedException( nature.toString() );
		}

		// Must do first -- sorting/ordering can determine the resolved type
		// (ex: Set vs. SortedSet).
		bindSortingAndOrdering( attributeBinding, attributeSource );

		if ( attributeSource.getFilterSources() != null ) {
			for ( final FilterSource filterSource : attributeSource.getFilterSources() ) {
				attributeBinding.addFilterConfiguration( createFilterConfiguration( filterSource, null ) );
			}
		}

		// Note: Collection types do not have a relational model
		attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
		attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );
		if ( attributeSource.getFetchStyle() == FetchStyle.SUBSELECT ) {
			attributeBindingContainer.seekEntityBinding().setSubselectLoadableCollections( true );
		}

		attributeBinding.getCaching().overlay( attributeSource.getCaching() );

		if ( StringHelper.isNotEmpty( attributeSource.getCustomPersisterClassName() ) ) {
			attributeBinding.setExplicitPersisterClass(
					localBindingContext().getClassLoaderAccess().<CollectionPersister>classForName(
							attributeSource.getCustomPersisterClassName()
					)
			);
		}
		attributeBinding.setCustomLoaderName( attributeSource.getCustomLoaderName() );
		attributeBinding.setCustomSqlInsert( attributeSource.getCustomSqlInsert() );
		attributeBinding.setCustomSqlUpdate( attributeSource.getCustomSqlUpdate() );
		attributeBinding.setCustomSqlDelete( attributeSource.getCustomSqlDelete() );
		attributeBinding.setCustomSqlDeleteAll( attributeSource.getCustomSqlDeleteAll() );
		attributeBinding.setWhere( attributeSource.getWhere() );
		attributeBinding.setMutable( attributeSource.isMutable() );
		attributeBinding.setBatchSize( attributeSource.getBatchSize() );
		ClassLoaderService cls = localBindingContext().getServiceRegistry().getService( ClassLoaderService.class );
		ReflectedCollectionJavaTypes reflectedCollectionJavaTypes = HibernateTypeHelper.getReflectedCollectionJavaTypes(
				cls,
				attributeBinding
		);
		switch ( attributeSource.getElementSource().getNature() ) {
			case BASIC:
				bindBasicPluralAttribute( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
				break;
			case ONE_TO_MANY:
				bindOneToManyAttribute( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
				break;
			case MANY_TO_MANY:
				bindManyToManyAttribute( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
				break;
			case AGGREGATE:
				bindPluralAggregateAttribute( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
				break;
			case MANY_TO_ANY:
				//todo??
			default:
				throw localBindingContext().makeMappingException(
						String.format(
								"Unknown type of collection element: %s",
								attributeSource.getElementSource().getNature()
						)
				);
		}
		// Cannot resolve plural attribute type until after the element binding is bound.
		final Type resolvedType = typeHelper().resolvePluralType( cls, attributeBinding, attributeSource, nature );
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		JavaTypeDescriptor defaultCollectionTypeDescriptor = typeHelper().reflectedCollectionClassReference(
				reflectedCollectionJavaTypes
		);
		if ( defaultCollectionTypeDescriptor == null ) {
			defaultCollectionTypeDescriptor = localBindingContext().typeDescriptor(
					attributeSource.getNature().reportedJavaType().getName()
			);
		}
		typeHelper().bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				attributeSource.getTypeInformation(),
				defaultCollectionTypeDescriptor,
				resolvedType
		);
		if ( attributeBinding.hasIndex() ) {
			bindPluralAttributeIndex(
					(IndexedPluralAttributeSource) attributeSource,
					(IndexedPluralAttributeBinding) attributeBinding,
					reflectedCollectionJavaTypes
			);
		}
		bindCollectionTablePrimaryKey( attributeBinding, attributeSource );
		rootBindingContext.getMetadataCollector().addCollection( attributeBinding );
		return attributeBinding;
	}

	private AbstractPluralAttributeBinding bindBagAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource,
			PluralAttribute attribute) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createBag( attributeSource.getName() );
		}
		return attributeBindingContainer.makeBagAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath()
		);
	}

	private AbstractPluralAttributeBinding bindListAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final IndexedPluralAttributeSource attributeSource,
			PluralAttribute attribute) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createList( attributeSource.getName() );
		}
		return attributeBindingContainer.makeListAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath(),
				getSequentialPluralAttributeIndexBase( attributeSource )
		);
	}

	private AbstractPluralAttributeBinding bindArrayAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final IndexedPluralAttributeSource attributeSource,
			PluralAttribute attribute) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createArray( attributeSource.getName() );
		}
		return attributeBindingContainer.makeArrayAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath(),
				getSequentialPluralAttributeIndexBase( attributeSource )
		);
	}

	private int getSequentialPluralAttributeIndexBase(IndexedPluralAttributeSource pluralAttributeSource) {
		final PluralAttributeIndexSource indexedPluralAttributeSource =  pluralAttributeSource.getIndexSource();
		if ( ! PluralAttributeSequentialIndexSource.class.isInstance( indexedPluralAttributeSource ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Expected an argument of type: %s; instead, got %s",
							PluralAttributeSequentialIndexSource.class.getName(),
							indexedPluralAttributeSource.getClass().getName()
					)
			);
		}
		return ( (PluralAttributeSequentialIndexSource) indexedPluralAttributeSource ).base();
	}

	private AbstractPluralAttributeBinding bindMapAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource,
			PluralAttribute attribute) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createMap( attributeSource.getName() );
		}
		return attributeBindingContainer.makeMapAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				pluralAttributeIndexNature( attributeSource ),
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath()
		);
	}

	private AbstractPluralAttributeBinding bindSetAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource,
			PluralAttribute attribute) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createSet( attributeSource.getName() );
		}
		return attributeBindingContainer.makeSetAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getAttributeRole(),
				attributeSource.getAttributePath()
		);
	}


	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ collection attributes binding

	private void bindBasicCollectionElement(
			final PluralAttributeElementBindingBasic elementBinding,
			final PluralAttributeElementSourceBasic elementSource,
			final JavaTypeDescriptor defaultElementJavaClassReference) {
		bindBasicPluralElementRelationalValues( elementSource, elementBinding );
		typeHelper().bindBasicCollectionElementType( elementBinding, elementSource, defaultElementJavaClassReference );
		elementBinding.getPluralAttributeBinding().getAttribute().setElementType(
				localBindingContext().makeDomainType(
						elementBinding.getHibernateTypeDescriptor().getJavaTypeDescriptor().getName()
				)
		);
	}

	private void bindNonAssociationCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource) {
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementNature.BASIC &&
				attributeSource.getElementSource().getNature() != PluralAttributeElementNature.AGGREGATE ) {
			throw new AssertionFailure(
					String.format(
							"Expected basic or aggregate attribute binding; instead got {%s}",
							attributeSource.getElementSource().getNature()
					)
			);
		}
		attributeBinding.getPluralAttributeKeyBinding().setInverse( false );
		TableSpecification collectionTable = tableHelper().createTable(
				attributeSource.getCollectionTableSpecificationSource(),
				new CollectionTableNamingStrategyHelper( attributeBinding )
		);
		if ( StringHelper.isNotEmpty( attributeSource.getCollectionTableComment() ) ) {
			collectionTable.addComment( attributeSource.getCollectionTableComment() );
		}
		if ( StringHelper.isNotEmpty( attributeSource.getCollectionTableCheck() ) ) {
			collectionTable.addCheckConstraint( attributeSource.getCollectionTableCheck() );
		}
		bindCollectionTableJoinRelationalValueBindings(
				attributeBinding,
				attributeSource,
				collectionTable
		);
	}

	private Aggregate findOrBuildAggregate(JavaTypeDescriptor embeddableDescriptor) {
		org.hibernate.metamodel.spi.domain.Type domainType = localBindingContext().locateOrBuildDomainType(
				embeddableDescriptor,
				true
		);

		if ( !Aggregate.class.isInstance( domainType ) ) {
			throw new IllegalStateException(
					String.format(
							Locale.ENGLISH,
							"Found previously built domain type for Aggregate JavaTypeDescriptor [%s] which was not Aggregate : %s",
							embeddableDescriptor.getName(),
							domainType.getName()
					)
			);
		}

		return (Aggregate) domainType;
	}

	private void bindCompositeCollectionElement(
			final PluralAttributeElementBindingEmbedded elementBinding,
			final PluralAttributeElementSourceEmbedded elementSource,
			final JavaTypeDescriptor reflectedElementTypeDescriptor) {
		final PluralAttributeBinding pluralAttributeBinding = elementBinding.getPluralAttributeBinding();
		// Create the aggregate type
		final JavaTypeDescriptor elementTypeDescriptor;
		if ( elementSource.getEmbeddableSource().getTypeDescriptor() != null ) {
			elementTypeDescriptor = elementSource.getEmbeddableSource().getTypeDescriptor();
		}
		else {
			elementTypeDescriptor = reflectedElementTypeDescriptor;
		}

		final Aggregate aggregate = findOrBuildAggregate( elementTypeDescriptor );

		final String parentReferenceAttributeName = StringHelper.nullIfEmpty(
				elementSource.getEmbeddableSource().getParentReferenceAttributeName()
		);
		final SingularAttribute parentAttribute = parentReferenceAttributeName == null
						? null
						: aggregate.createSingularAttribute( parentReferenceAttributeName );

		Class<? extends ComponentTuplizer> tuplizerClass = null;
		final String tuplizerClassName = elementSource.getEmbeddableSource().getExplicitTuplizerClassName();
		if ( StringHelper.isNotEmpty( tuplizerClassName ) ) {
			tuplizerClass = localBindingContext().getBuildingOptions()
					.getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( tuplizerClassName );
		}

		final EmbeddableBinding embeddableBinding = elementBinding.createBindingContainer(
				aggregate,
				createMetaAttributeContext(
						pluralAttributeBinding.getContainer(),
						elementSource.getToolingHintSources()
				),
				parentAttribute,
				tuplizerClass
		);

		bindAttributes( embeddableBinding, elementSource.getEmbeddableSource() );

		pluralAttributeBinding.getAttribute().setElementType( aggregate );
		Type resolvedType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel( rootBindingContext.getServiceRegistry(), embeddableBinding, false, false )
		);
		// TODO: binding the HibernateTypeDescriptor should be simplified since we know the class name already
		typeHelper().bindHibernateTypeDescriptor(
				elementBinding.getHibernateTypeDescriptor(),
				aggregate.getDescriptor().getName().toString(),
				null,
				reflectedElementTypeDescriptor == null ? null : reflectedElementTypeDescriptor,
				resolvedType
		);
		/**
		 * TODO
		 * don't know why, but see org.hibernate.mapping.Property#getCompositeCascadeStyle
		 *
		 * and not sure if this is the right place to apply this logic, apparently source level is not okay, so here it is, for now.
		 */
		for ( AttributeBinding ab : embeddableBinding.attributeBindings() ) {
			if ( ab.isCascadeable() ) {
				final Cascadeable cascadeable;
				if ( ab.getAttribute().isSingular() ) {
					cascadeable = Cascadeable.class.cast( ab );
				}
				else {
					cascadeable = Cascadeable.class.cast( ( (PluralAttributeBinding) ab ).getPluralAttributeElementBinding() );
				}
				CascadeStyle cascadeStyle = cascadeable.getCascadeStyle();
				if ( cascadeStyle != CascadeStyles.NONE ) {
					elementBinding.setCascadeStyle( CascadeStyles.ALL );
				}
			}
		}
		if ( elementBinding.getCascadeStyle() == null || elementBinding.getCascadeStyle() == CascadeStyles.NONE ) {
			elementBinding.setCascadeStyle( determineCascadeStyle( elementSource.getCascadeStyles() ) );
		}
	}

	private void bindListIndex(
			IndexedPluralAttributeBinding attributeBinding,
			PluralAttributeSequentialIndexSource indexSource,
			JavaTypeDescriptor reflectedIndexTypeDescriptor) {
		final BasicPluralAttributeIndexBinding indexBinding =
				(BasicPluralAttributeIndexBinding) attributeBinding.getPluralAttributeIndexBinding();
		// TODO: need to resolve default column names.
		indexBinding.setRelationalValueBindings(
				relationalValueBindingHelper().createRelationalValueBindings(
						attributeBinding.getContainer(),
						indexSource,
						attributeBinding.getPluralAttributeKeyBinding().getCollectionTable(),
						indexSource.getDefaultNamingStrategies(),
						true
				)
		);

		typeHelper().bindHibernateTypeDescriptor(
				indexBinding.getHibernateTypeDescriptor(),
				indexSource.getTypeInformation(),
				Primitives.INTEGER.getPrimitiveType()
		);
		typeHelper().bindJdbcDataType(
				indexBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				indexBinding.getRelationalValueBindings()
		);

		IndexedPluralAttribute indexedPluralAttribute =
				(IndexedPluralAttribute) indexBinding.getIndexedPluralAttributeBinding().getAttribute();

		// todo : we need to centralize this stuff somewhere...
		final HibernateTypeDescriptor hibernateTypeDescriptor = indexBinding.getHibernateTypeDescriptor();
		final BasicType basicType;
		if ( hibernateTypeDescriptor.getJavaTypeDescriptor() != null ) {
			basicType = (BasicType) localBindingContext().makeDomainType(
					hibernateTypeDescriptor.getJavaTypeDescriptor().getName()
			);
		}
		else if ( hibernateTypeDescriptor.getResolvedTypeMapping() != null ) {
			basicType = (BasicType) localBindingContext().makeDomainType(
					hibernateTypeDescriptor.getResolvedTypeMapping().getName()
			);
		}
		else if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
			basicType = (BasicType) localBindingContext().makeDomainType(
					hibernateTypeDescriptor.getExplicitTypeName()
			);
		}
		else {
			basicType = new BasicType( hibernateTypeDescriptor.getJavaTypeDescriptor() );
		}

		indexedPluralAttribute.setIndexType( basicType );
	}

	private void bindEntityAttributePluralAttributeIndex(
			final IndexedPluralAttributeBinding attributeBinding,
			final PluralAttributeMapKeySourceEntityAttribute indexSource,
			final JavaTypeDescriptor defaultIndexTypeDescriptor) {
		throw new NotYetImplementedException( "Plural attribute index that is an attribute of the referenced entity is not supported yet." );
	}

	private void bindBasicCollectionIndex(
			final IndexedPluralAttributeBinding attributeBinding,
			final PluralAttributeMapKeySourceBasic indexSource,
			final JavaTypeDescriptor defaultIndexTypeDescriptor) {
		final BasicPluralAttributeIndexBinding indexBinding =
				(BasicPluralAttributeIndexBinding) attributeBinding.getPluralAttributeIndexBinding();
		// TODO: need to resolve default column names.
		indexBinding.setRelationalValueBindings(
				relationalValueBindingHelper().createRelationalValueBindings(
						attributeBinding.getContainer(),
						indexSource,
						attributeBinding.getPluralAttributeKeyBinding().getCollectionTable(),
						indexSource.getDefaultNamingStrategies(),
						attributeBinding.getPluralAttributeElementBinding()
								.getNature() != PluralAttributeElementNature.ONE_TO_MANY
				)
		);
		// TODO: create a foreign key if non-inverse and the index is an association

		typeHelper().bindHibernateTypeDescriptor(
				indexBinding.getHibernateTypeDescriptor(),
				indexSource.getTypeInformation(),
				defaultIndexTypeDescriptor
		);
		typeHelper().bindJdbcDataType(
				indexBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				indexBinding.getRelationalValueBindings()
		);
		IndexedPluralAttribute indexedPluralAttribute =
				(IndexedPluralAttribute) indexBinding.getIndexedPluralAttributeBinding().getAttribute();

		// todo : we need to centralize this stuff somewhere...
		final HibernateTypeDescriptor hibernateTypeDescriptor = indexBinding.getHibernateTypeDescriptor();
		final BasicType basicType;
		if ( hibernateTypeDescriptor.getJavaTypeDescriptor() != null ) {
			basicType = (BasicType) localBindingContext().makeDomainType(
					hibernateTypeDescriptor.getJavaTypeDescriptor().getName()
			);
		}
		else if ( hibernateTypeDescriptor.getResolvedTypeMapping() != null ) {
			basicType = (BasicType) localBindingContext().makeDomainType(
					hibernateTypeDescriptor.getResolvedTypeMapping().getName()
			);
		}
		else if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
			basicType = (BasicType) localBindingContext().makeDomainType(
					hibernateTypeDescriptor.getExplicitTypeName()
			);
		}
		else {
			basicType = new BasicType( hibernateTypeDescriptor.getJavaTypeDescriptor() );
		}

		indexedPluralAttribute.setIndexType( basicType );
	}

	private void bindCompositeCollectionIndex(
		final CompositePluralAttributeIndexBinding indexBinding,
		final IndexedPluralAttributeSource indexedPluralAttributeSource,
		final JavaTypeDescriptor reflectedIndexTypeDescriptor) {
		final PluralAttributeMapKeySourceEmbedded indexSource =
				(PluralAttributeMapKeySourceEmbedded) indexedPluralAttributeSource.getIndexSource();
		final JavaTypeDescriptor indexTypeDescriptor;
		if ( indexSource.getEmbeddableSource().getTypeDescriptor() != null ) {
			indexTypeDescriptor = indexSource.getEmbeddableSource().getTypeDescriptor();
		}
		else {
			indexTypeDescriptor = reflectedIndexTypeDescriptor;
		}

		final Aggregate aggregate = findOrBuildAggregate( indexTypeDescriptor );

		final EmbeddableBinding embeddableBinding = indexBinding.createCompositeAttributeBindingContainer(
				aggregate,
				null,
				null,
				null
		);

		bindAttributes( embeddableBinding, indexSource.getEmbeddableSource() );

		Type resolvedType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel(
						rootBindingContext.getServiceRegistry(),
						embeddableBinding,
						false,
						false
				)
		);
		// TODO: binding the HibernateTypeDescriptor should be simplified since we know the class name already
		typeHelper().bindHibernateTypeDescriptor(
				indexBinding.getHibernateTypeDescriptor(),
				aggregate.getDescriptor().getName().toString(),
				null,
				indexTypeDescriptor == null ? null : indexTypeDescriptor,
				resolvedType
		);
		IndexedPluralAttribute indexedPluralAttribute =
				(IndexedPluralAttribute) indexBinding.getIndexedPluralAttributeBinding().getAttribute();
		indexedPluralAttribute.setIndexType( aggregate );
	}

	private void bindOneToManyCollectionElement(
			final PluralAttributeElementBindingOneToMany elementBinding,
			final PluralAttributeElementSourceOneToMany elementSource,
			final EntityBinding referencedEntityBinding,
			final JavaTypeDescriptor defaultElementTypeDescriptor) {
		elementBinding.setElementEntityIdentifier(
				referencedEntityBinding.getKeyRelationalValueBindings()
		);

		Type resolvedElementType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().manyToOne(
				referencedEntityBinding.getEntityName(),
				true,
				null,
				false,
				false,
				elementSource.isIgnoreNotFound(),
				false
		);
		final HibernateTypeDescriptor hibernateTypeDescriptor = elementBinding.getHibernateTypeDescriptor();
		final JavaTypeDescriptor elementTypeDescriptor;
		if ( defaultElementTypeDescriptor != null ) {
			elementTypeDescriptor = defaultElementTypeDescriptor;
		}
		else {
			elementTypeDescriptor = referencedEntityBinding.getEntity().getDescriptor();
		}
		typeHelper().bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				referencedEntityBinding.getEntityName(),
				null,
				elementTypeDescriptor,
				resolvedElementType
		);
		// no need to bind JDBC data types because element is referenced EntityBinding's ID
		elementBinding.setCascadeStyle( determineCascadeStyle( elementSource.getCascadeStyles() ) );
		elementBinding.getPluralAttributeBinding().getAttribute().setElementType( referencedEntityBinding.getEntity() );
	}

	private void bindManyToManyCollectionElement(
			final PluralAttributeElementBindingManyToMany elementBinding,
			final PluralAttributeElementSourceManyToMany elementSource,
			final EntityBinding referencedEntityBinding,
			final JavaTypeDescriptor defaultElementTypeDescriptor) {
		final TableSpecification collectionTable =
				elementBinding.getPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable();
		final AssociationRelationalBindingResolver resolver = getAssociationRelationalBindingResolver(
				elementSource.getAttributeSource()
		);
		final List<RelationalValueBinding> relationalValueBindings =
				resolver.resolveManyToManyElementRelationalValueBindings(
						elementBinding.getPluralAttributeBinding().getContainer().seekEntityBinding(),
						elementSource,
						collectionTable,
						referencedEntityBinding
				);
		final ForeignKey foreignKey;
		if ( !elementBinding.getPluralAttributeBinding().getPluralAttributeKeyBinding().isInverse() &&
				!relationalValueBindingHelper().hasDerivedValue( relationalValueBindings ) ) {
			foreignKey = resolver.resolveManyToManyElementForeignKey(
					elementBinding.getPluralAttributeBinding().getContainer().seekEntityBinding(),
					elementSource,
					collectionTable,
					relationalValueBindings,
					referencedEntityBinding
			);
		}
		else {
			foreignKey = null;
		}
		if ( elementSource.isUnique() ) {
			final List<Column> columns = new ArrayList<Column>();
			for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
				if ( ! relationalValueBinding.isDerived() )  {
					columns.add( (Column) relationalValueBinding.getValue() );
				}
			}
			tableHelper().createUniqueKey( collectionTable, columns, null );
		}
		elementBinding.setJoinRelationalValueBindings( relationalValueBindings, foreignKey );
		typeHelper().bindManyToManyAttributeType(
				elementBinding,
				elementSource,
				referencedEntityBinding,
				defaultElementTypeDescriptor
		);
		elementBinding.getPluralAttributeBinding().getAttribute().setElementType( referencedEntityBinding.getEntity() );
		elementBinding.setCascadeStyle( determineCascadeStyle( elementSource.getCascadeStyles() ) );
		elementBinding.setManyToManyWhere( elementSource.getWhere() );
		elementBinding.setManyToManyOrderBy( elementSource.getOrder() );
		elementBinding.setFetchMode( elementSource.getFetchTiming() == FetchTiming.IMMEDIATE ? FetchMode.JOIN : FetchMode.SELECT );
		//TODO: initialize filters from elementSource
	}

	private void bindOneToManyCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource,
			final EntityBinding referencedEntityBinding) {
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementNature.ONE_TO_MANY ) {
			throw new AssertionFailure(
					String.format(
							"Expected one-to-many attribute binding; instead got {%s}",
							attributeSource.getElementSource().getNature()
					)
			);
		}
		// By definition, a one-to-many can only be on a foreign key, so the
		// collection table is the referenced entity binding's primary table.
		final TableSpecification collectionTable = referencedEntityBinding.getPrimaryTable();
		final boolean isInverse = attributeSource.isInverse();
		final PluralAttributeKeyBinding keyBinding = attributeBinding.getPluralAttributeKeyBinding();
		keyBinding.setInverse( isInverse );
		bindCollectionTableJoinRelationalValueBindings( attributeBinding, attributeSource, collectionTable );
	}

	private void bindManyToManyCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource,
			final EntityBinding referencedEntityBinding) {
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementNature.MANY_TO_MANY ) {
			throw new AssertionFailure(
					String.format(
							"Expected many-to-many attribute binding; instead got {%s}",
							attributeSource.getElementSource().getNature()
					)
			);
		}
		final AssociationRelationalBindingResolver resolver = getAssociationRelationalBindingResolver( attributeSource );
		final TableSpecification collectionTable = resolver.resolveManyToManyCollectionTable(
				attributeSource,
				attributeSource.getAttributePath().getFullPath(),
				attributeBinding.getContainer().seekEntityBinding(),
				referencedEntityBinding
		);
		final PluralAttributeKeyBinding keyBinding = attributeBinding.getPluralAttributeKeyBinding();
		keyBinding.setInverse( attributeSource.isInverse() );
		bindCollectionTableJoinRelationalValueBindings( attributeBinding, attributeSource, collectionTable );
	}

	private void bindPluralAttributeIndex(
			final IndexedPluralAttributeSource attributeSource,
			final IndexedPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final JavaTypeDescriptor reflectedIndexTypeDescriptor = typeHelper().reflectedCollectionIndexClassReference(
				reflectedCollectionJavaTypes
		);
		final PluralAttributeIndexSource indexSource = attributeSource.getIndexSource();
		if ( PluralAttributeMapKeySourceEntityAttribute.class.isInstance( indexSource ) ) {
			bindEntityAttributePluralAttributeIndex(
					attributeBinding,
					(PluralAttributeMapKeySourceEntityAttribute) indexSource,
					reflectedIndexTypeDescriptor
			);
		}
		else {
			switch ( attributeSource.getIndexSource().getNature() ) {
				case BASIC: {
					bindBasicCollectionIndex(
							attributeBinding,
							(PluralAttributeMapKeySourceBasic) attributeSource.getIndexSource(),
							reflectedIndexTypeDescriptor
					);
					break;
				}
				case AGGREGATE: {
					bindCompositeCollectionIndex(
							(CompositePluralAttributeIndexBinding) attributeBinding.getPluralAttributeIndexBinding(),
							attributeSource,
							reflectedIndexTypeDescriptor
					);
					break;
				}
				case SEQUENTIAL: {
					// easy peasy, we have an array/list index
					bindListIndex(
							attributeBinding,
							(PluralAttributeSequentialIndexSource) attributeSource.getIndexSource(),
							reflectedIndexTypeDescriptor
					);
					break;
				}
				default: {
					throw new NotYetImplementedException(
							String.format(
									"%s collection indexes are not supported yet.",
									attributeSource.getIndexSource().getNature()
							)
					);
				}
			}
			if ( attributeBinding.getPluralAttributeElementBinding()
					.getNature() == PluralAttributeElementNature.ONE_TO_MANY ) {
				for ( RelationalValueBinding relationalValueBinding : attributeBinding.getPluralAttributeIndexBinding().getRelationalValueBindings() ) {
					if ( Column.class.isInstance( relationalValueBinding.getValue() ) ) {
						// TODO: fix this when column nullability is refactored
						Column column = (Column) relationalValueBinding.getValue();
						column.setNullable( true );
					}
				}
			}
		}
	}

	private void bindPluralAggregateAttribute(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		bindNonAssociationCollectionKey( attributeBinding, attributeSource );
		bindCompositeCollectionElement(
				(PluralAttributeElementBindingEmbedded) attributeBinding.getPluralAttributeElementBinding(),
				(PluralAttributeElementSourceEmbedded) attributeSource.getElementSource(),
				typeHelper().reflectedCollectionElementJavaType( reflectedCollectionJavaTypes )
		);
	}

	private void bindManyToManyAttribute(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final PluralAttributeElementSourceManyToMany elementSource =
				(PluralAttributeElementSourceManyToMany) attributeSource.getElementSource();
		final JavaTypeDescriptor reflectedElementTypeDescriptor = typeHelper().reflectedCollectionElementJavaType(
				reflectedCollectionJavaTypes
		);
		if ( elementSource.getReferencedEntityName() == null && reflectedElementTypeDescriptor == null ) {
			throw localBindingContext().makeMappingException(
					String.format(
							"The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
							attributeSource.getAttributeRole().getFullPath()
					)
			);
		}
		EntityBinding referencedEntityBinding = locateEntityBinding(
				reflectedElementTypeDescriptor,
				elementSource.getReferencedEntityName()
		);
		PluralAttributeElementBindingManyToMany manyToManyPluralAttributeElementBinding = (PluralAttributeElementBindingManyToMany) attributeBinding
				.getPluralAttributeElementBinding();

		if ( elementSource.getFilterSources() != null ) {
			for ( FilterSource filterSource : elementSource.getFilterSources() ) {
				manyToManyPluralAttributeElementBinding.addFilterConfiguration(
						createFilterConfiguration(
								filterSource,
								null
						)
				);
			}
		}
		bindManyToManyCollectionKey( attributeBinding, attributeSource, referencedEntityBinding );
		bindManyToManyCollectionElement(
				manyToManyPluralAttributeElementBinding,
				(PluralAttributeElementSourceManyToMany) attributeSource.getElementSource(),
				referencedEntityBinding,
				reflectedElementTypeDescriptor
		);
	}

	private void bindOneToManyAttribute(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final PluralAttributeElementSourceOneToMany elementSource =
				(PluralAttributeElementSourceOneToMany) attributeSource.getElementSource();
		final JavaTypeDescriptor defaultElementTypeDescriptor = typeHelper().reflectedCollectionElementJavaType(
				reflectedCollectionJavaTypes
		);
		if ( elementSource.getReferencedEntityName() == null && defaultElementTypeDescriptor == null ) {
			throw localBindingContext().makeMappingException(
					String.format(
							"The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
							attributeSource.getAttributeRole().getFullPath()
					)
			);
		}
		EntityBinding referencedEntityBinding = locateEntityBinding(
				defaultElementTypeDescriptor,
				elementSource.getReferencedEntityName()
		);
		bindOneToManyCollectionKey( attributeBinding, attributeSource, referencedEntityBinding );
		bindOneToManyCollectionElement(
				(PluralAttributeElementBindingOneToMany) attributeBinding.getPluralAttributeElementBinding(),
				(PluralAttributeElementSourceOneToMany) attributeSource.getElementSource(),
				referencedEntityBinding,
				defaultElementTypeDescriptor
		);
	}

	private void bindBasicPluralAttribute(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		bindNonAssociationCollectionKey( attributeBinding, attributeSource );
		bindBasicCollectionElement(
				(PluralAttributeElementBindingBasic) attributeBinding.getPluralAttributeElementBinding(),
				(PluralAttributeElementSourceBasic) attributeSource.getElementSource(),
				typeHelper().reflectedCollectionElementJavaType( reflectedCollectionJavaTypes )
		);
	}


	private void bindSortingAndOrdering(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource) {
		if ( Sortable.class.isInstance( attributeSource ) ) {
			final Sortable sortable = (Sortable) attributeSource;
			attributeBinding.setSorted( sortable.isSorted() );
			if ( sortable.isSorted()
					&& !sortable.getComparatorName().equalsIgnoreCase( "natural" ) ) {
				Class<Comparator<?>> comparatorClass = localBindingContext().
						getClassLoaderAccess()
						.classForName( sortable.getComparatorName() );
				try {
					attributeBinding.setComparator( comparatorClass.newInstance() );
				}
				catch ( Exception error ) {
					throw localBindingContext().makeMappingException(
							String.format(
									"Unable to create comparator [%s] for attribute [%s]",
									sortable.getComparatorName(),
									attributeSource.getName()
							),
							error
					);
				}
			}
		}
		if ( Orderable.class.isInstance( attributeSource ) ) {
			final Orderable orderable = (Orderable) attributeSource;
			if ( orderable.isOrdered() ) {
				attributeBinding.setOrderBy( orderable.getOrder() );

			}
		}
	}

	private SingularAttributeBinding determinePluralAttributeKeyReferencedBinding(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource) {
		return getAssociationRelationalBindingResolver( attributeSource ).resolvePluralAttributeKeyReferencedBinding(
				attributeBindingContainer,
				attributeSource
		);
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ relational binding relates methods
	private void bindBasicPluralElementRelationalValues(
			final RelationalValueSourceContainer relationalValueSourceContainer,
			final PluralAttributeElementBindingBasic elementBinding) {
		elementBinding.setRelationalValueBindings(
				relationalValueBindingHelper().createRelationalValueBindings(
						elementBinding.getPluralAttributeBinding().getContainer(),
						relationalValueSourceContainer,
						elementBinding.getPluralAttributeBinding().getAttribute(),
						elementBinding.getPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable(),
						false
				)
		);
	}

	private void bindSetCollectionTablePrimaryKey(final SetBinding attributeBinding) {
		// only bind the primary key if there is a non-nullable element value.
		if ( !attributeBinding.getPluralAttributeElementBinding().getRelationalValueContainer().hasNonNullableRelationalValueBinding() ) {
			return;
		}


		final PluralAttributeElementBinding elementBinding = attributeBinding.getPluralAttributeElementBinding();
		final PrimaryKey primaryKey = attributeBinding.getPluralAttributeKeyBinding()
				.getCollectionTable()
				.getPrimaryKey();
		final List<RelationalValueBinding> keyValueBindings =
				attributeBinding.getPluralAttributeKeyBinding().getRelationalValueBindings();
		for ( final RelationalValueBinding keyRelationalValueBinding : keyValueBindings ) {
			primaryKey.addColumn( (Column) keyRelationalValueBinding.getValue() );
		}
		for ( final RelationalValueBinding elementValueBinding
				: elementBinding.getRelationalValueContainer().relationalValueBindings() ) {
			if ( !elementValueBinding.isDerived() && !elementValueBinding.isNullable() ) {
				primaryKey.addColumn( (Column) elementValueBinding.getValue() );
			}
		}
	}

	private void bindCollectionTableJoinRelationalValueBindings(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource,
			final TableSpecification collectionTable) {

		final EntityBinding referencedEntityBinding = attributeBinding.getContainer().seekEntityBinding();
		final PluralAttributeKeyBinding keyBinding = attributeBinding.getPluralAttributeKeyBinding();
		final AssociationRelationalBindingResolver resolver = getAssociationRelationalBindingResolver( attributeSource );
		final List<RelationalValueBinding> sourceRelationalBindings =
				resolver.resolvePluralAttributeKeyRelationalValueBindings(
						attributeSource,
						attributeBinding.getContainer().seekEntityBinding(),
						collectionTable,
						referencedEntityBinding
				);

		final ForeignKey foreignKey = resolver.resolvePluralAttributeKeyForeignKey(
				attributeSource,
				attributeBinding.getContainer().seekEntityBinding(),
				collectionTable,
				sourceRelationalBindings,
				referencedEntityBinding
		);
		keyBinding.setJoinRelationalValueBindings( sourceRelationalBindings, foreignKey );

		final HibernateTypeDescriptor pluralAttributeKeyTypeDescriptor = keyBinding.getHibernateTypeDescriptor();

		pluralAttributeKeyTypeDescriptor.copyFrom(
				keyBinding.getReferencedAttributeBinding()
						.getHibernateTypeDescriptor()
		);
		final Type resolvedKeyType = pluralAttributeKeyTypeDescriptor.getResolvedTypeMapping();
		typeHelper().bindJdbcDataType( resolvedKeyType, keyBinding.getRelationalValueBindings() );
	}

	/**
	 * TODO : It is really confusing that we have so many different <tt>natures</tt>
	 */
	private void bindCollectionTablePrimaryKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource) {
		final PluralAttributeNature pluralAttributeSourceNature = attributeSource.getNature();
		final PluralAttributeElementNature pluralElementSourceNature = attributeSource.getElementSource().getNature();

		//TODO what is this case? it would be really good to add a comment
		if ( pluralElementSourceNature == PluralAttributeElementNature.ONE_TO_MANY
				|| pluralAttributeSourceNature == PluralAttributeNature.BAG ) {
			return;
		}
		if ( !attributeBinding.getPluralAttributeKeyBinding().isInverse() ) {
			switch ( pluralAttributeSourceNature ) {
				case SET:
					bindSetCollectionTablePrimaryKey( (SetBinding) attributeBinding );
					break;
				case LIST:
				case MAP:
				case ARRAY:
					bindIndexedCollectionTablePrimaryKey( (IndexedPluralAttributeBinding) attributeBinding );
					break;
				default:
					throw new NotYetImplementedException(
							String.format( "%s of elements is not supported yet.", pluralAttributeSourceNature )
					);
			}
		}
	}

	private TableSpecification findConstraintTable(EntityBinding entityBinding, String tableName) {
		try {
			return entityBinding.locateTable( tableName );
		}
		catch ( AssertionFailure e ) {
			Identifier identifier = Identifier.toIdentifier( tableName );
			
			// TODO: Make TableGenerator & MultipleHiLoPerTableGenerator extend an abstract?
			// @TableGenerator
			IdentifierGenerator idGenerator = entityBinding.getHierarchyDetails().getEntityIdentifier().getIdentifierGenerator();
			if (idGenerator instanceof TableGenerator) {
				Table generatorTable = ((TableGenerator) idGenerator).getTable();
				if (generatorTable != null && generatorTable.getLogicalName().equals( identifier ) ) {
					return generatorTable;
				}
			}
			else if (idGenerator instanceof MultipleHiLoPerTableGenerator) {
				Table generatorTable = ((MultipleHiLoPerTableGenerator) idGenerator).getTable();
				if (generatorTable != null && generatorTable.getLogicalName().equals( identifier ) ) {
					return generatorTable;
				}
			}
			
			// @JoinTable and @CollectionTable
			for ( AttributeBinding attributeBinding : entityBinding.attributeBindings() ) {
				if ( attributeBinding instanceof PluralAttributeBinding ) {
					PluralAttributeBinding pluralAttributeBinding = (PluralAttributeBinding) attributeBinding;
					final TableSpecification pluralTable =
							pluralAttributeBinding.getPluralAttributeKeyBinding().getCollectionTable();
					if ( pluralTable != null && pluralTable.getLogicalName().equals( identifier ) ) {
						return pluralTable;
					}
				}
			}
		}
		
		throw new AssertionFailure(
				String.format(
						"Unable to find locate table %s",
						tableName
				)
		);
	}


	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ simple instance helper methods

	private AssociationRelationalBindingResolver getAssociationRelationalBindingResolver(AttributeSource attributeSource) {
		if ( ToOneAttributeSource.class.isInstance( attributeSource ) &&
				ToOneAttributeSource.class.cast( attributeSource ).isMappedBy() ) {
			return mappedByAssociationRelationalBindingResolver;
		}
		else if ( PluralAttributeSource.class.isInstance( attributeSource ) &&
				PluralAttributeSource.class.cast( attributeSource ).getMappedBy() != null ) {
			return mappedByAssociationRelationalBindingResolver;
		}
		else {
			return standardAssociationRelationalBindingResolver;

		}
	}


	private String propertyAccessorName(AttributeSource attributeSource) {
		return propertyAccessorName( attributeSource.getPropertyAccessorName(), rootBindingContext );
	}

	public static String propertyAccessorName(AttributeSource attributeSource, BinderRootContext context) {
		return propertyAccessorName( attributeSource.getPropertyAccessorName(), context );
	}

	private String propertyAccessorName(String propertyAccessorName) {
		return propertyAccessorName( propertyAccessorName, rootBindingContext );
	}

	public static String propertyAccessorName(String propertyAccessorName, BinderRootContext context) {
		if ( propertyAccessorName != null ) {
			return propertyAccessorName;
		}

		return context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
				.getMappingDefaults()
				.getPropertyAccessorName();
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ static methods
	private static TableSpecification locateDefaultTableSpecificationForAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource) {
		return attributeSource.getContainingTableName() == null ?
				attributeBindingContainer.getPrimaryTable() :
				attributeBindingContainer.seekEntityBinding().locateTable( attributeSource.getContainingTableName() );
	}

	private static PluralAttributeElementNature pluralAttributeElementNature(
			final PluralAttributeSource attributeSource) {
		return attributeSource.getElementSource().getNature();
	}

	private static PluralAttributeIndexNature pluralAttributeIndexNature(
			final PluralAttributeSource attributeSource) {
		if ( !IndexedPluralAttributeSource.class.isInstance( attributeSource ) ) {
			return null;
		}
		return PluralAttributeIndexNature.valueOf(
				( (IndexedPluralAttributeSource) attributeSource ).getIndexSource().getNature().name()
		);
	}

	private void bindIndexedCollectionTablePrimaryKey(
			final IndexedPluralAttributeBinding attributeBinding) {
		final TableSpecification collectionTable =  attributeBinding.getPluralAttributeKeyBinding().getCollectionTable();
		final PrimaryKey primaryKey = collectionTable.getPrimaryKey();

		final List<RelationalValueBinding> keyRelationalValueBindings =
				attributeBinding.getPluralAttributeKeyBinding().getRelationalValueBindings();

		final PluralAttributeIndexBinding indexBinding = attributeBinding.getPluralAttributeIndexBinding();

		// Don't add any columns to the primary key unless there is a non-nullable index or element column.
		if ( relationalValueBindingHelper().hasDerivedValue( indexBinding.getRelationalValueBindings() ) ) {
			// the index has as formula;
			// only create the primary key if there are non-nullable element columns
			final PluralAttributeElementBinding pluralAttributeElementBinding =  attributeBinding.getPluralAttributeElementBinding();
			if ( pluralAttributeElementBinding.getRelationalValueContainer().hasNonNullableRelationalValueBinding() ) {
				for ( final RelationalValueBinding keyRelationalValueBinding : keyRelationalValueBindings ) {
					primaryKey.addColumn( (Column) keyRelationalValueBinding.getValue() );
				}
				for ( RelationalValueBinding relationalValueBinding
						: pluralAttributeElementBinding.getRelationalValueContainer().relationalValueBindings() ) {
					if ( !relationalValueBinding.isDerived() &&
							!relationalValueBinding.isNullable() &&
							relationalValueBinding.getTable().equals( collectionTable ) ){
						primaryKey.addColumn( (Column)relationalValueBinding.getValue() );
					}
				}
			}
		}
		else {
			for ( final RelationalValueBinding keyRelationalValueBinding : keyRelationalValueBindings ) {
				primaryKey.addColumn( (Column) keyRelationalValueBinding.getValue() );
			}
			for ( RelationalValueBinding relationalValueBinding : indexBinding.getRelationalValueBindings() ) {
				if ( relationalValueBinding.getTable().equals( collectionTable ) &&
						!relationalValueBinding.isNullable() ) {
					primaryKey.addColumn( (Column) relationalValueBinding.getValue() );
				}
			}
		}
	}

	// TODO: should this be moved to CascadeStyles as a static method?
	// TODO: sources already factor in default cascade; should that be done here instead?
	private static CascadeStyle determineCascadeStyle(
			final Iterable<CascadeStyle> cascadeStyles) {
		CascadeStyle cascadeStyleResult;
		List<CascadeStyle> cascadeStyleList = new ArrayList<CascadeStyle>();
		for ( CascadeStyle style : cascadeStyles ) {
			if ( style != CascadeStyles.NONE ) {
				cascadeStyleList.add( style );
			}
		}
		if ( cascadeStyleList.isEmpty() ) {
			cascadeStyleResult = CascadeStyles.NONE;
		}
		else if ( cascadeStyleList.size() == 1 ) {
			cascadeStyleResult = cascadeStyleList.get( 0 );
		}
		else {
			cascadeStyleResult = new CascadeStyles.MultipleCascadeStyle(
					cascadeStyleList.toArray( new CascadeStyle[cascadeStyleList.size()] )
			);
		}
		return cascadeStyleResult;
	}

	public static MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSource attributeSource) {
		return createMetaAttributeContext( attributeBindingContainer, attributeSource.getToolingHintSources() );
	}

	public static MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final Iterable<? extends ToolingHintSource> metaAttributeSources) {
		return createMetaAttributeContext(
				metaAttributeSources,
				false,
				attributeBindingContainer.getMetaAttributeContext()
		);
	}

	public static MetaAttributeContext createMetaAttributeContext(
			final Iterable<? extends ToolingHintSource> metaAttributeSources,
			final boolean onlyInheritable,
			final MetaAttributeContext parentContext) {
		final MetaAttributeContext subContext = new MetaAttributeContext( parentContext );
		for ( final ToolingHintSource toolingHintSource : metaAttributeSources ) {
			if ( onlyInheritable && !toolingHintSource.isInheritable() ) {
				continue;
			}
			final String name = toolingHintSource.getName();
			MetaAttribute metaAttribute = subContext.getLocalMetaAttribute( name );
			if ( metaAttribute == null || metaAttribute == parentContext.getMetaAttribute( name ) ) {
				metaAttribute = new MetaAttribute( name );
				subContext.add( metaAttribute );
			}
			metaAttribute.addValue( toolingHintSource.getValue() );
		}
		return subContext;
	}

	public static interface DefaultNamingStrategy {
		String defaultName(NamingStrategy namingStrategy);
	}

	protected static SingularAttribute createSingularAttribute(
			AttributeBindingContainer attributeBindingContainer,
			SingularAttributeSource attributeSource) {
		return attributeSource.isVirtualAttribute()
				? attributeBindingContainer.getAttributeContainer()
				.createSyntheticSingularAttribute( attributeSource.getName() )
				: attributeBindingContainer.getAttributeContainer()
				.createSingularAttribute( attributeSource.getName() );
	}


	public static String interpretIdentifierUnsavedValue(
			final IdentifierSource identifierSource,
			final IdentifierGeneratorDefinition generator) {
		if ( identifierSource == null ) {
			throw new IllegalArgumentException( "identifierSource must be non-null." );
		}
		if ( generator == null || StringHelper.isEmpty( generator.getStrategy() ) ) {
			throw new IllegalArgumentException( "generator must be non-null and its strategy must be non-empty." );
		}
		String unsavedValue = null;
		if ( identifierSource.getUnsavedValue() != null ) {
			unsavedValue = identifierSource.getUnsavedValue();
		}
		else if ( "assigned".equals( generator.getStrategy() ) ) {
			unsavedValue = "undefined";
		}
		else {
			switch ( identifierSource.getNature() ) {
				case SIMPLE: {
					// unsavedValue = null;
					break;
				}
				case NON_AGGREGATED_COMPOSITE: {
					// The generator strategy should be "assigned" and processed above.
					throw new IllegalStateException(
							String.format(
									"Expected generator strategy for composite ID: 'assigned'; instead it is: %s",
									generator.getStrategy()
							)
					);
				}
				case AGGREGATED_COMPOSITE: {
					// TODO: if the component only contains 1 attribute (when flattened)
					// and it is not an association then null should be returned;
					// otherwise "undefined" should be returned.
					throw new NotYetImplementedException(
							String.format(
									"Unsaved value for (%s) identifier not implemented yet.",
									identifierSource.getNature()
							)
					);
				}
				default: {
					throw new AssertionFailure(
							String.format(
									"Unexpected identifier nature: %s",
									identifierSource.getNature()
							)
					);
				}
			}
		}
		return unsavedValue;
	}


















	/**
	 * The naming here is probably backwards...
	 *
	 * Represents a process within a binding step.
	 */
	private static interface Process {
		public boolean doProcess();

		public void addFollowOnProcess(Process followOn);
	}

	private static abstract class AbstractProcess implements Process {
		private List<Process> followOnProcesses;

		@Override
		public final void addFollowOnProcess(Process followOn) {
			if ( followOnProcesses == null ) {
				followOnProcesses = new ArrayList<Process>();
			}
			followOnProcesses.add( followOn );
		}

		protected void doFollowOnProcesses() {
			if ( followOnProcesses == null ) {
				return;
			}

			for ( Process followOnProcess : followOnProcesses ) {
				followOnProcess.doProcess();
			}
			followOnProcesses.clear();
		}
	}

	private static abstract class AbstractNoFollowOnProcess implements Process {
		@Override
		public final void addFollowOnProcess(Process followOn) {
			throw new IllegalStateException( "Follow on processes not allowed here" );
		}
	}

	private static interface IdentifierBinder extends Process {
	}

	private static abstract class AbstractIdentifierBinder extends AbstractProcess implements IdentifierBinder {
		protected final HierarchyDetails binding;
		protected final BinderLocalBindingContext context;

		protected AbstractIdentifierBinder(
				HierarchyDetails binding,
				BinderLocalBindingContext context) {
			this.binding = binding;
			this.context = context;
		}

		protected String determineReferencedEntityName(ToOneAttributeSource toOneAttributeSource) {
			final JavaTypeDescriptor referencedEntityTypeDescriptor = context.typeHelper().determineJavaType(
					toOneAttributeSource,
					binding.getRootEntityBinding().getAttributeContainer()
			);
			return Binder.determineReferencedEntityName(
					referencedEntityTypeDescriptor,
					toOneAttributeSource.getReferencedEntityName()
			);
		}

		protected boolean isIdentifierFullyBound(String referencedEntityName) {
			final EntityBinding referencedEntityBinding = context.getMetadataCollector()
					.getEntityBinding( referencedEntityName );
			if ( referencedEntityBinding == null ) {
				return false;
			}

			return referencedEntityBinding.getHierarchyDetails()
					.getEntityIdentifier()
					.isBound();
		}

		protected void afterBindingComplete() {
			doFollowOnProcesses();
		}
	}

	/**
	 * Binder step for binding identifiers, pks and secondary tables across
	 * an entity hierarchy.
	 */
	private class IdentifierAndSecondaryTableBindingStep implements BinderStepCombinedStrategy {
		private final EntityHierarchySource source;
		private final HierarchyDetails binding;

		private IdentifierBinder pendingIdentifierBinder;

		private IdentifierAndSecondaryTableBindingStep(EntityHierarchySource source) {
			this.source = source;
			this.binding = rootBindingContext.locateBinding( source );
		}

		@Override
		public void visit(EntityHierarchySource source, BinderLocalBindingContext context) {
			if ( source != this.source ) {
				return;
			}

			// Build an id binder, and ask it to process the binding
			final IdentifierBinder idBinder = makeIdentifierBinder( binding, source, context );
			idBinder.addFollowOnProcess(
					new AbstractNoFollowOnProcess() {
						final EntityBinding rootEntityBinding = binding.getRootEntityBinding();
						final EntitySource rootEntitySource = IdentifierAndSecondaryTableBindingStep.this.source.getRoot();
						@Override
						public boolean doProcess() {
							bindSecondaryTables( rootEntityBinding, rootEntitySource );
							return false;
						}
					}
			);
			final boolean delayed = !idBinder.doProcess();
			if ( delayed ) {
				// the binding we not able to be processed yet, delay it and
				// keep the binder reference around to register follow-ons as
				// needed.
				//
				// the binder should have registered itself as a listener with
				// the event bus for any events it is waiting on
				pendingIdentifierBinder = idBinder;
			}
		}

		private IdentifierBinder makeIdentifierBinder(
				HierarchyDetails binding,
				EntityHierarchySource source,
				BinderLocalBindingContext context) {
			final IdentifierSource identifierSource = source.getIdentifierSource();
			final EntityIdentifierNature nature = identifierSource.getNature();

			switch ( nature ) {
				case SIMPLE: {
					return new SimpleIdentifierBinder( binding, (SimpleIdentifierSource) identifierSource, context );
				}
				case AGGREGATED_COMPOSITE: {
					return new AggregatedCompositeIdentifierBinder(
							binding,
							(AggregatedCompositeIdentifierSource) identifierSource,
							context
					);
				}
				case NON_AGGREGATED_COMPOSITE: {
					return new NonAggregatedCompositeIdentifierBinder(
							binding,
							(NonAggregatedCompositeIdentifierSource) identifierSource,
							context
					);
				}
				default: {
					throw localBindingContext().makeMappingException( "Unknown identifier nature : " + nature.name() );
				}
			}
		}

		/**
		 * Binder for binding simple (single basic or to-one attribute) identifiers
		 */
		private class SimpleIdentifierBinder extends AbstractIdentifierBinder {
			protected final SimpleIdentifierSource identifierSource;

			public SimpleIdentifierBinder(
					HierarchyDetails binding,
					SimpleIdentifierSource identifierSource,
					BinderLocalBindingContext context) {
				super( binding, context );

				this.identifierSource = identifierSource;
			}

			@Override
			public boolean doProcess() {
				// opt out of completing the process if we cannot...
				final SingularAttributeSource attributeSource = identifierSource.getIdentifierAttributeSource();
				if ( attributeSource.getSingularAttributeNature() == SingularAttributeNature.MANY_TO_ONE
						|| attributeSource.getSingularAttributeNature() == SingularAttributeNature.ONE_TO_ONE ) {
					final ToOneAttributeSource toOneAttributeSource = (ToOneAttributeSource) attributeSource;
					final String referencedEntityName = determineReferencedEntityName( toOneAttributeSource );
					if ( !isIdentifierFullyBound( referencedEntityName ) ) {
						delayBinding( binding, referencedEntityName );
						return false;
					}
				}

				completeBinding();
				return true;
			}

			private void delayBinding(final HierarchyDetails hierarchyDetails, final String referencedEntityName) {
				context.getEventBus().addIdentifierBindingListener(
						new IdentifierBindingListener() {
							@Override
							public boolean identifierResolved(HierarchyDetails resolvingHierarchyDetails) {
								if ( hierarchyDetails == resolvingHierarchyDetails ) {
									// avoid cycle
									return false;
								}

								if ( isIdentifierFullyBound( referencedEntityName ) ) {
									// complete the binding, and tell the event bus to
									// remove us as a listener
									completeBinding();
									return true;
								}
								return false;
							}
						}
				);
			}

			private boolean inCompletion = false;

			private void completeBinding() {
				// avoid cycle
				if ( inCompletion ) {
					return;
				}

				inCompletion = true;

				final SingularAttributeBinding idAttributeBinding = bindIdentifierAttribute(
						binding.getRootEntityBinding(),
						identifierSource.getIdentifierAttributeSource()
				);

				// Configure ID generator
				IdentifierGeneratorDefinition generator = identifierSource.getIdentifierGeneratorDescriptor();
				if ( generator == null ) {
					final Map<String, String> params = new HashMap<String, String>();
					params.put( IdentifierGenerator.ENTITY_NAME, binding.getRootEntityBinding().getEntityName() );
					generator = new IdentifierGeneratorDefinition( "default_assign_identity_generator", "assigned", params );
				}

				// determine the unsaved value mapping
				final String unsavedValue = interpretIdentifierUnsavedValue( identifierSource, generator );

				binding.getEntityIdentifier().prepareAsSimpleIdentifier(
						idAttributeBinding,
						generator,
						unsavedValue
				);

				afterBindingComplete();
			}

		}

		private class AggregatedCompositeIdentifierBinder extends AbstractIdentifierBinder {
			private final AggregatedCompositeIdentifierSource identifierSource;

			public AggregatedCompositeIdentifierBinder(
					HierarchyDetails binding,
					AggregatedCompositeIdentifierSource identifierSource,
					BinderLocalBindingContext context) {
				super( binding, context );

				this.identifierSource = identifierSource;
			}

			@Override
			public boolean doProcess() {
				final Set<String> pendingEntityNames = new HashSet<String>();

				collectPendingEntityNames( pendingEntityNames, identifierSource.getIdentifierAttributeSource() );

				for ( MapsIdSource mapsIdSource : identifierSource.getMapsIdSources() ) {
					final ToOneAttributeSource toOneAttributeSource = mapsIdSource.getAssociationAttributeSource();
					final String referencedEntityName = determineReferencedEntityName( toOneAttributeSource );
					if ( !isIdentifierFullyBound( referencedEntityName ) ) {
						pendingEntityNames.add( referencedEntityName );
					}
				}

				if ( !pendingEntityNames.isEmpty() ) {
					delayBinding( binding, pendingEntityNames );
					return false;
				}

				completeBinding();
				return true;
			}

			private void collectPendingEntityNames(
					Set<String> pendingEntityNames,
					EmbeddedAttributeSource embeddedAttributeSource) {
				for ( AttributeSource attributeSource :
						embeddedAttributeSource.getEmbeddableSource().attributeSources() ) {
					final SingularAttributeSource sas = (SingularAttributeSource) attributeSource;
					if ( sas.getSingularAttributeNature() == SingularAttributeNature.MANY_TO_ONE
							|| sas.getSingularAttributeNature() == SingularAttributeNature.ONE_TO_ONE ) {
						final ToOneAttributeSource toOneAttributeSource = (ToOneAttributeSource) sas;
						final String referencedEntityName = determineReferencedEntityName( toOneAttributeSource );
						if ( !isIdentifierFullyBound( referencedEntityName ) ) {
							pendingEntityNames.add( referencedEntityName );
						}
					}
					else if ( sas.getSingularAttributeNature() == SingularAttributeNature.COMPOSITE ) {
						collectPendingEntityNames( pendingEntityNames, (EmbeddedAttributeSource) sas );
					}
				}
			}

			private void delayBinding(final HierarchyDetails hierarchyDetails, final Set<String> names) {
				context.getEventBus().addIdentifierBindingListener(
						new IdentifierBindingListener() {
							private final Set<String> pendingEntityNames = names;
							@Override
							public boolean identifierResolved(HierarchyDetails resolvingHierarchyDetails) {
								if ( hierarchyDetails == resolvingHierarchyDetails ) {
									// avoid cycle
									return false;
								}

								final Iterator<String> itr = pendingEntityNames.iterator();
								while ( itr.hasNext() ) {
									final String pendingEntityName = itr.next();
									if ( isIdentifierFullyBound( pendingEntityName ) ) {
										itr.remove();
									}
								}
								if ( pendingEntityNames.isEmpty() ) {
									// complete the binding, and tell the event bus to
									// remove us as a listener
									completeBinding();
									return true;
								}

								return false;
							}
						}
				);
			}


			private boolean inCompletion = false;

			private void completeBinding() {
				// avoid cycle
				if ( inCompletion ) {
					return;
				}

				inCompletion = true;

				final EmbeddedAttributeSource cidAttrSource = identifierSource.getIdentifierAttributeSource();

				// Process any associations annotated as @MapsId, adding them
				// to a Map keyed by the name of the id attribute they map..
				// we'll use this info later when binding the pk class attributes
				final Map<String,SingularAssociationAttributeBinding> mapsIdMap =
						new HashMap<String, SingularAssociationAttributeBinding>();
				for ( MapsIdSource mapsIdSource : identifierSource.getMapsIdSources() ) {
					final SingularAttributeBinding attrBinding = bindIdentifierAttribute(
							binding.getRootEntityBinding(),
							mapsIdSource.getAssociationAttributeSource()
					);
					mapsIdMap.put(
							mapsIdSource.getMappedIdAttributeName(),
							(SingularAssociationAttributeBinding) attrBinding
					);
				}

				// Resolve the domain definition of the PK class
				final Aggregate pkClass;
				if ( source.getEntityMode() == EntityMode.MAP ) {
					pkClass = (Aggregate) localBindingContext().locateOrBuildDomainType(
							new DynamicTypeDescriptorImpl( DotName.createSimple( "CID" ), null ),
							true
					);
				}
				else {
					final JavaTypeDescriptor pkClassTypeDescriptor = determinePkClassTypeDescriptor(
							binding.getRootEntityBinding(),
							identifierSource
					);
					pkClass = (Aggregate) localBindingContext().locateOrBuildDomainType(
							pkClassTypeDescriptor,
							true
					);
				}


				// `pkAttribute` is the (domain) attribute reference for the @EmbeddedId attribute
				SingularAttribute pkAttribute = binding.getRootEntityBinding()
						.getEntity()
						.locateCompositeAttribute( cidAttrSource.getName() );
				if ( pkAttribute == null ) {
					pkAttribute = binding.getRootEntityBinding().getEntity()
							.createSingularAttribute( cidAttrSource.getName() );
					pkAttribute.resolveType( pkClass );
				}

				// Handle any `parent` reference
				SingularAttribute pkClassParentAttribute = null;
				if ( cidAttrSource.getEmbeddableSource().getParentReferenceAttributeName() != null ) {
					pkClassParentAttribute = pkClass.createSingularAttribute(
							cidAttrSource.getEmbeddableSource().getParentReferenceAttributeName()
					);
				}

				// Handle custom tuplizer
				Class<? extends ComponentTuplizer> customTuplizer = null;
				if ( cidAttrSource.getEmbeddableSource().getExplicitTuplizerClassName() != null ) {
					final ClassLoaderService cls = rootBindingContext.getServiceRegistry().getService( ClassLoaderService.class );
					customTuplizer = cls.classForName( cidAttrSource.getEmbeddableSource().getExplicitTuplizerClassName() );
				}

				// Build the attribute binding for the @EmbeddedId attribute; builds the
				// EmbeddableBinding implicitly, which we store immediately after as
				// `pkClassBinding`
				final EmbeddedAttributeBinding idAttributeBinding = binding.getRootEntityBinding().makeAggregatedCompositeAttributeBinding(
						pkAttribute,
						pkClassParentAttribute,
						customTuplizer,
						cidAttrSource.getPropertyAccessorName(),
						false,
						false,
						NaturalIdMutability.NOT_NATURAL_ID,
						null,
						cidAttrSource.getAttributeRole(),
						cidAttrSource.getAttributePath()
				);
				final EmbeddableBinding pkClassBinding = idAttributeBinding.getEmbeddableBinding();

				// Now process each attribute in the PK class
				for ( AttributeSource attributeSource : cidAttrSource.getEmbeddableSource().attributeSources() ) {
					final SingularAttributeSource singularAttributeSource = (SingularAttributeSource) attributeSource;
					final String attributeName = singularAttributeSource.getName();

					// Did the attribute have corresponding @MapsId?
					final SingularAssociationAttributeBinding correspondingAttribute = mapsIdMap.get( attributeName );
					if ( correspondingAttribute == null ) {
						// there was no corresponding @MapsId
						final SingularAttributeBinding singularAttributeBinding = bindSingularAttribute(
								pkClassBinding,
								singularAttributeSource,
								true
						);
					}
					else {
						// there was a corresponding @MapsId, grab its
						// relational value information
						final List<RelationalValueBinding> relationalValueBindings = new ArrayList<RelationalValueBinding>();
						for ( RelationalValueBinding relationalValueBinding : correspondingAttribute.getRelationalValueBindings() ) {
							final RelationalValueBinding copy;
							if ( Column.class.isInstance( relationalValueBinding.getValue() ) ) {
								copy = new RelationalValueBinding(
										relationalValueBinding.getTable(),
										(Column) relationalValueBinding.getValue(),
										false,
										false
								);
							}
							else {
								copy = new RelationalValueBinding(
										relationalValueBinding.getTable(),
										(DerivedValue) relationalValueBinding.getValue()
								);
							}
							relationalValueBindings.add( copy );
						}
						final SingularAttributeBinding pkClassAttributeBinding = new BasicAttributeBinding(
								pkClassBinding,
								pkClass.createSingularAttribute( attributeName ),
								relationalValueBindings,
								singularAttributeSource.getPropertyAccessorName(),
								false,
								false,
								NaturalIdMutability.NOT_NATURAL_ID,
								null,
								singularAttributeSource.getAttributeRole(),
								singularAttributeSource.getAttributePath(),
								PropertyGeneration.NEVER
						);
						pkClassAttributeBinding.getHibernateTypeDescriptor().copyFrom(
								correspondingAttribute.getReferencedAttributeBinding().getHibernateTypeDescriptor()
						);
					}
				}

				// Configure ID generator
				IdentifierGeneratorDefinition generator = identifierSource.getIdentifierGeneratorDescriptor();
				if ( generator == null ) {
					final Map<String, String> params = new HashMap<String, String>();
					params.put( IdentifierGenerator.ENTITY_NAME, binding.getRootEntityBinding().getEntityName() );
					generator = new IdentifierGeneratorDefinition( "default_assign_identity_generator", "assigned", params );
				}

				// determine the unsaved value mapping
				final String unsavedValue = interpretIdentifierUnsavedValue( identifierSource, generator );

				binding.getEntityIdentifier().prepareAsAggregatedCompositeIdentifier(
						idAttributeBinding,
						generator,
						unsavedValue
				);

				typeHelper().bindAggregatedCompositeAttributeType(
						localBindingContext().getServiceRegistry(),
						true,
						pkClass,
						pkClass.getDescriptor(),
						idAttributeBinding
				);

				afterBindingComplete();
			}

			private JavaTypeDescriptor determinePkClassTypeDescriptor(
					EntityBinding rootEntityBinding,
					AggregatedCompositeIdentifierSource identifierSource) {
				final EmbeddedAttributeSource cidAttrSource = identifierSource.getIdentifierAttributeSource();
				// we can know the pk-class name in one of 2 ways:
				//		1) If the composite-id attribute mapping named it as a "target"
				if ( cidAttrSource.getEmbeddableSource().getTypeDescriptor() != null ) {
					return cidAttrSource.getEmbeddableSource().getTypeDescriptor();
				}

				//		2) so long as the entity is not dynamic (MAP mode), we
				//		can use reflite on the entity type.
				final JavaTypeDescriptor entityType = rootEntityBinding.getEntity().getDescriptor();
				if ( entityType != null ) {
					final MemberDescriptor cidAttrDescriptor = findAttributeMemberDescriptor(
							entityType,
							cidAttrSource.getName()
					);
					if ( cidAttrDescriptor == null ) {
						throw context.makeMappingException(
								"Could not locate aggregated (@EmbeddedId) composite-identifier attribute : " +
										cidAttrSource.getAttributeRole().getFullPath()
						);
					}
					return cidAttrDescriptor.getType().getErasedType();
				}

				throw context.makeMappingException(
						"Could not determine aggregated (@EmbeddedId) composite-identifier type : " +
								cidAttrSource.getAttributeRole().getFullPath()
				);
			}

			private MemberDescriptor findAttributeMemberDescriptor(JavaTypeDescriptor entityType, String name) {
				for ( MethodDescriptor methodDescriptor : entityType.getDeclaredMethods() ) {
					if ( !AbstractPersistentAttributeMemberResolver.isPersistable( methodDescriptor ) ) {
						continue;
					}

					final String attributeName = ReflectHelper.getPropertyNameFromGetterMethod( methodDescriptor.getName() );

					if ( attributeName.equals( name ) ) {
						return methodDescriptor;
					}
				}

				for ( FieldDescriptor fieldDescriptor : entityType.getDeclaredFields() ) {
					if ( !AbstractPersistentAttributeMemberResolver.isPersistable( fieldDescriptor ) ) {
						continue;
					}

					if ( fieldDescriptor.getName().equals( name ) ) {
						return fieldDescriptor;
					}
				}

				if ( ClassDescriptor.class.isInstance( entityType ) ) {
					return findAttributeMemberDescriptor(
							( (ClassDescriptor) entityType ).getSuperType(),
							name
					);
				}

				return null;
			}
		}

		private class NonAggregatedCompositeIdentifierBinder extends AbstractIdentifierBinder {
			private final NonAggregatedCompositeIdentifierSource identifierSource;

			public NonAggregatedCompositeIdentifierBinder(
					HierarchyDetails binding,
					NonAggregatedCompositeIdentifierSource identifierSource,
					BinderLocalBindingContext context) {
				super( binding, context );
				this.identifierSource = identifierSource;
			}

			@Override
			public boolean doProcess() {
				final Set<String> pendingEntityNames = new HashSet<String>();
				for ( SingularAttributeSource sas : identifierSource.getAttributeSourcesMakingUpIdentifier() ) {
					if ( sas.getSingularAttributeNature() == SingularAttributeNature.MANY_TO_ONE
							|| sas.getSingularAttributeNature() == SingularAttributeNature.ONE_TO_ONE ) {
						final ToOneAttributeSource toOneAttributeSource = (ToOneAttributeSource) sas;
						final String referencedEntityName = determineReferencedEntityName( toOneAttributeSource );
						if ( !isIdentifierFullyBound( referencedEntityName ) ) {
							pendingEntityNames.add( referencedEntityName );
						}
					}
				}

				if ( !pendingEntityNames.isEmpty() ) {
					delayBinding( binding, pendingEntityNames );
					return false;
				}

				completeBinding();
				return true;
			}

			private void delayBinding(final HierarchyDetails hierarchyDetails, final Set<String> names) {
				context.getEventBus().addIdentifierBindingListener(
						new IdentifierBindingListener() {
							private final Set<String> pendingEntityNames = names;
							@Override
							public boolean identifierResolved(HierarchyDetails resolvingHierarchyDetails) {
								if ( hierarchyDetails == resolvingHierarchyDetails ) {
									// avoid cycle
									return false;
								}
								final Iterator<String> itr = pendingEntityNames.iterator();
								while ( itr.hasNext() ) {
									final String pendingEntityName = itr.next();
									if ( isIdentifierFullyBound( pendingEntityName ) ) {
										itr.remove();
									}
								}
								if ( pendingEntityNames.isEmpty() ) {
									// complete the binding, and tell the event bus to
									// remove us as a listener
									completeBinding();
									return true;
								}

								return false;
							}
						}
				);
			}


			private boolean inCompletion = false;

			private void completeBinding() {
				// avoid cycle
				if ( inCompletion ) {
					return;
				}

				inCompletion = true;

				// locate the attribute bindings for the real attributes
				final Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();
				for ( SingularAttributeSource attributeSource : identifierSource.getAttributeSourcesMakingUpIdentifier() ) {
					final SingularAttributeBinding singularAttributeBinding = bindIdentifierAttribute(
							binding.getRootEntityBinding(),
							attributeSource
					);
					attributeBindingMap.put( attributeSource.getName(), singularAttributeBinding );
				}

				// Configure ID generator
				IdentifierGeneratorDefinition generator = identifierSource.getIdentifierGeneratorDescriptor();
				if ( generator == null ) {
					final Map<String, String> params = new HashMap<String, String>();
					params.put( IdentifierGenerator.ENTITY_NAME, binding.getRootEntityBinding().getEntityName() );
					generator = new IdentifierGeneratorDefinition( "default_assign_identity_generator", "assigned", params );
				}

				final EmbeddableBindingImplementor virtualEmbeddableBinding = new AbstractEmbeddableBinding(
						binding.getRootEntityBinding(),
						binding.getRootEntityBinding().getAttributeContainer(),
						binding.getRootEntityBinding().getPrimaryTable(),
						binding.getRootEntityBinding().getRoleBase().append( "<IdClass>" ),
						binding.getRootEntityBinding().getPathBase().append( "<IdClass>" ),
						createMetaAttributeContext(
								binding.getRootEntityBinding(),
								identifierSource.getToolingHintSources()
						),
						null,
						null) {
					@Override
					protected boolean isModifiable() {
						return false;
					}

					@Override
					protected Map<String, AttributeBinding> attributeBindingMapInternal() {
						return attributeBindingMap;
					}

					@Override
					public boolean isAggregated() {
						return false;
					}

					@Override
					public JavaTypeDescriptor getTypeDescriptor() {
						return binding.getRootEntityBinding().getEntity().getDescriptor();
					}
				};

				final EmbeddableBindingImplementor idClassBinding;
				if ( identifierSource.getIdClassSource() != null ) {
					final Aggregate component = (Aggregate) localBindingContext().locateOrBuildDomainType(
							identifierSource.getIdClassSource().getTypeDescriptor(),
							true
					);
					idClassBinding = new IdClassEmbeddableBinding(
							binding.getRootEntityBinding(),
							identifierSource.getIdClassSource(),
							component,
							virtualEmbeddableBinding
					);
				}
				else {
					idClassBinding = null;
				}

				// Create the synthetic attribute
				final SingularAttribute virtualAttribute = binding.getRootEntityBinding().getEntity().createSyntheticCompositeAttribute(
						SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME,
						binding.getRootEntityBinding().getEntity()
				);

				// NOTE : legacy code used the IdClass composite (if one) as:
				//		1) virtual id attribute type
				//		2) the reported id type of the entity (persister)
				final EmbeddedAttributeBinding virtualAttributeBinding = binding.getRootEntityBinding()
						.makeVirtualCompositeAttributeBinding(
								virtualAttribute,
								idClassBinding == null ? virtualEmbeddableBinding : idClassBinding,
								createMetaAttributeContext(
										binding.getRootEntityBinding(),
										identifierSource.getToolingHintSources()
								)
						);




				// Create the synthetic attribute binding.
				NonAggregatedCompositeIdentifierBinding idBinding =
						binding.getEntityIdentifier().prepareAsNonAggregatedCompositeIdentifier(
								virtualEmbeddableBinding,
								virtualAttributeBinding,
								idClassBinding,
								generator,
								interpretIdentifierUnsavedValue( identifierSource, generator )
						);

				// todo : is this needed?  The type is implied for both composites here..
				typeHelper().bindNonAggregatedCompositeIdentifierType(
						localBindingContext().getServiceRegistry(),
						idBinding
				);

				afterBindingComplete();
			}
		}

		class IdClassEmbeddableBinding extends AbstractEmbeddableBinding {
			private final JavaTypeDescriptor idClassType;
			private final Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();
			private boolean modifiable = true;

			public IdClassEmbeddableBinding(
					EntityBinding rootEntityBinding,
					EmbeddableSource idClassSource,
					Aggregate composite,
					EmbeddableBindingImplementor virtualIdentifierComposite) {
				super(
						rootEntityBinding,
						composite,
						rootEntityBinding.getPrimaryTable(),
						new AttributeRole( idClassSource.getTypeDescriptor().getName().toString() ),
						new AttributePath(),
						null,
						null,
						null
				);

				this.idClassType = idClassSource.getTypeDescriptor();

				// todo : as we build these, we need a way to apply:
				// 		1) column naming overrides (virtualIdentifierComposite)
				// 		2) MapsId

				// one option is a Builder like I did for annotation sources...


				for ( AttributeSource attributeSource : idClassSource.attributeSources() ) {
					final String attributeName = attributeSource.getName();

					final SingularAttributeBinding correspondingAttribute =
							(SingularAttributeBinding) virtualIdentifierComposite.locateAttributeBinding( attributeName );
					if ( correspondingAttribute == null ) {
						throw localBindingContext().makeMappingException(
								"Could not locate Entity attribute corresponding to IdClass attribute : " +
										idClassType.getName().toString() + '.' + attributeName
						);
					}

					SingularAttribute idClassSubAttribute = composite.locateSingularAttribute( attributeName );
					if ( idClassSubAttribute == null ) {
						idClassSubAttribute = composite.createSingularAttribute( attributeName );
					}
					final SingularAttributeBinding idClassAttributeBinding = new BasicAttributeBinding(
							this,
							idClassSubAttribute,
							correspondingAttribute.getRelationalValueBindings(),
							correspondingAttribute.getPropertyAccessorName(),
							false,
							false,
							NaturalIdMutability.NOT_NATURAL_ID,
							null,
							getRoleBase().append( attributeName ),
							getPathBase().append( attributeName ),
							PropertyGeneration.NEVER
					);

					if ( BasicAttributeBinding.class.isInstance( correspondingAttribute )
							|| EmbeddedAttributeBinding.class.isInstance( correspondingAttribute ) ) {
						idClassAttributeBinding.getHibernateTypeDescriptor().copyFrom(
								correspondingAttribute.getHibernateTypeDescriptor()
						);
					}
					else {
						idClassAttributeBinding.getHibernateTypeDescriptor().copyFrom(
								( (SingularAssociationAttributeBinding) correspondingAttribute )
										.getReferencedAttributeBinding()
										.getHibernateTypeDescriptor()
						);
					}

					attributeBindingMap.put( attributeName, idClassAttributeBinding );
				}

				modifiable = false;
			}

			@Override
			protected boolean isModifiable() {
				return modifiable;
			}

			@Override
			protected Map<String, AttributeBinding> attributeBindingMapInternal() {
				return attributeBindingMap;
			}

			@Override
			public boolean isAggregated() {
				return false;
			}

			@Override
			public JavaTypeDescriptor getTypeDescriptor() {
				return idClassType;
			}
		}


		@Override
		public boolean applyToRootEntity() {
			return false;
		}

		@Override
		public void visit(final EntitySource source, BinderLocalBindingContext context) {
			final EntityBinding binding = context.locateBinding( source );

			AbstractNoFollowOnProcess process = new AbstractNoFollowOnProcess() {
				@Override
				public boolean doProcess() {
					bindSubEntityPrimaryKey( binding, source );
					bindSecondaryTables( binding, source );
					markSuperEntityTableAbstractIfNecessary( binding.getSuperEntityBinding() );
					return false;
				}
			};

			if ( pendingIdentifierBinder == null ) {
				process.doProcess();
			}
			else {
				pendingIdentifierBinder.addFollowOnProcess( process );
			}
		}

		@Override
		public void afterAllEntitiesInHierarchy() {
			if ( pendingIdentifierBinder == null ) {
				rootBindingContext.getEventBus().fireIdentifierResolved( binding );
			}
			else {
				pendingIdentifierBinder.addFollowOnProcess(
						new AbstractNoFollowOnProcess() {
							@Override
							public boolean doProcess() {
								localBindingContext().getEventBus().fireIdentifierResolved( binding );
								return false;
							}
						}
				);
			}
		}

		private void bindSubEntityPrimaryKey(EntityBinding entityBinding, EntitySource entitySource) {
			final InheritanceType inheritanceType = entityBinding.getHierarchyDetails().getInheritanceType();
			final EntityBinding superEntityBinding = entityBinding.getSuperEntityBinding();
			if ( superEntityBinding == null ) {
				throw new AssertionFailure( "super entitybinding is null " );
			}
			if ( inheritanceType == InheritanceType.JOINED ) {
				final JoinedSubclassEntitySource subclassEntitySource = (JoinedSubclassEntitySource) entitySource;
				final List<ColumnSource> columnSources = subclassEntitySource.getPrimaryKeyColumnSources();
				final TableSpecification table = entityBinding.getPrimaryTable();
				final List<RelationalValueBinding> joinRelationalValueBindings =
						getJoinedPrimaryKeyRelationalValueBindings( superEntityBinding, columnSources, table );

				entityBinding.setKeyRelationalValueBindings( joinRelationalValueBindings );

				final List<Column> targetColumns = foreignKeyHelper().determineForeignKeyTargetColumns(
						superEntityBinding,
						subclassEntitySource
				);

				ForeignKey foreignKey = locateOrCreateForeignKey(
						subclassEntitySource.getExplicitForeignKeyName(),
						table,
						joinRelationalValueBindings,
						// shouldn't the table here always be the root entity's primary table?
						foreignKeyHelper().determineForeignKeyTargetTable(
								superEntityBinding,
								subclassEntitySource
						),
						targetColumns,
						subclassEntitySource.isCascadeDeleteEnabled(),
						subclassEntitySource.createForeignKeyConstraint()
				);

				if ( subclassEntitySource.isCascadeDeleteEnabled() ) {
					entityBinding.setCascadeDeleteEnabled( true );
				}
			}
		}

		private void bindSecondaryTables(EntityBinding entityBinding, EntitySource entitySource) {
			for ( final SecondaryTableSource secondaryTableSource : entitySource.getSecondaryTables() ) {
				final TableSpecification table = tableHelper().createTable(
						secondaryTableSource.getTableSource(),
						new TableNamingStrategyHelper( entityBinding )
				);
				table.addComment( secondaryTableSource.getComment() );
				final List<RelationalValueBinding> joinRelationalValueBindings = getJoinedPrimaryKeyRelationalValueBindings(
						entityBinding,
						secondaryTableSource.getPrimaryKeyColumnSources(),
						table
				);

				// TODO: make the foreign key column the primary key???
				final List<Column> targetColumns = foreignKeyHelper()
						.determineForeignKeyTargetColumns( entityBinding, secondaryTableSource );
				final ForeignKey foreignKey = locateOrCreateForeignKey(
						secondaryTableSource.getExplicitForeignKeyName(),
						table,
						joinRelationalValueBindings,
						foreignKeyHelper().determineForeignKeyTargetTable( entityBinding, secondaryTableSource ),
						targetColumns,
						secondaryTableSource.isCascadeDeleteEnabled(),
						secondaryTableSource.createForeignKeyConstraint()
				);
				SecondaryTable secondaryTable = new SecondaryTable( table, foreignKey );
				if ( secondaryTableSource.getFetchStyle()!=null ) {
					secondaryTable.setFetchStyle( secondaryTableSource.getFetchStyle() );
				}
				secondaryTable.setInverse( secondaryTableSource.isInverse() );
				secondaryTable.setOptional( secondaryTableSource.isOptional() );
				secondaryTable.setCascadeDeleteEnabled( secondaryTableSource.isCascadeDeleteEnabled() );
				secondaryTable.setCustomDelete( secondaryTableSource.getCustomSqlDelete() );
				secondaryTable.setCustomInsert( secondaryTableSource.getCustomSqlInsert() );
				secondaryTable.setCustomUpdate( secondaryTableSource.getCustomSqlUpdate() );
				entityBinding.addSecondaryTable( secondaryTable );
				rootBindingContext.getMetadataCollector().addSecondaryTable( secondaryTable );
			}
		}

		private  void markSuperEntityTableAbstractIfNecessary(EntityBinding superEntityBinding) {
			if ( superEntityBinding == null ) {
				return;
			}
			if ( superEntityBinding.getHierarchyDetails().getInheritanceType() != InheritanceType.TABLE_PER_CLASS ) {
				return;
			}
			if ( superEntityBinding.isAbstract() != Boolean.TRUE ) {
				return;
			}
			if ( !Table.class.isInstance( superEntityBinding.getPrimaryTable() ) ) {
				return;
			}
			Table.class.cast( superEntityBinding.getPrimaryTable() ).setPhysicalTable( false );
		}
	}








	public static interface AttributeBindingBuilder {
		public AttributeBinding buildAttributeBinding(
				AttributeBindingContainer container,
				AttributeSource attributeSource,
				BinderRootContext context);

		public SingularAttributeBinding buildSingularAttributeBinding(
				AttributeBindingContainer container,
				SingularAttributeSource attributeSource,
				BinderRootContext context);

		public PluralAttributeBinding buildPluralAttributeBinding(
				AttributeBindingContainer container,
				PluralAttributeSource attributeSource,
				BinderRootContext context);


		public BasicAttributeBinding buildBasicAttributeBinding(
				AttributeBindingContainer container,
				SingularAttributeSource attributeSource,
				BinderRootContext context);

		public EmbeddedAttributeBinding buildCompositeAttributeBinding(
				AttributeBindingContainer container,
				EmbeddedAttributeSource attributeSource,
				BinderRootContext context);

		public OneToOneAttributeBinding buildOneToOneAttributeBinding(
				AttributeBindingContainer container,
				ToOneAttributeSource attributeSource,
				BinderRootContext context);

		public ManyToOneAttributeBinding buildManyToOneAttributeBinding(
				AttributeBindingContainer container,
				ToOneAttributeSource attributeSource,
				BinderRootContext context);

		public SingularAttributeBinding buildAnyAttributeBinding(
				AttributeBindingContainer container,
				SingularAttributeSource attributeSource,
				BinderRootContext context);

		public PluralAttributeBinding buildManyToAnyAttributeBinding(
				AttributeBindingContainer container,
				PluralAttributeSource attributeSource,
				BinderRootContext context);
	}

//	public static class StandardAttributeBindingBuilder implements AttributeBindingBuilder {
//		/**
//		 * Singleton access
//		 */
//		public static final StandardAttributeBindingBuilder INSTANCE = new StandardAttributeBindingBuilder();
//
//		private StandardAssociationRelationalBindingResolverImpl standardAssociationRelationalBindingResolver;
//		private MappedByAssociationRelationalBindingResolverImpl mappedByAssociationRelationalBindingResolver;
//
//		@Override
//		public AttributeBinding buildAttributeBinding(
//				AttributeBindingContainer container,
//				AttributeSource attributeSource,
//				BinderRootContext context) {
//			if ( attributeSource.isSingular() ) {
//				return buildSingularAttributeBinding(
//						container,
//						(SingularAttributeSource) attributeSource,
//						context
//				);
//			}
//			else {
//				return buildPluralAttributeBinding(
//						container,
//						(PluralAttributeSource) attributeSource,
//						context
//				);
//			}
//		}
//
//		@Override
//		public SingularAttributeBinding buildSingularAttributeBinding(
//				AttributeBindingContainer container,
//				SingularAttributeSource attributeSource,
//				BinderRootContext context) {
//			switch ( attributeSource.getSingularAttributeNature() ) {
//				case BASIC: {
//					return buildBasicAttributeBinding(
//							container,
//							attributeSource,
//							context
//					);
//				}
//				case COMPOSITE: {
//					return buildCompositeAttributeBinding(
//							container,
//							(EmbeddedAttributeSource) attributeSource,
//							context
//					);
//				}
//				case MANY_TO_ONE: {
//					return buildManyToOneAttributeBinding(
//							container,
//							(ToOneAttributeSource) attributeSource,
//							context
//					);
//				}
//				case ONE_TO_ONE: {
//					return buildOneToOneAttributeBinding(
//							container,
//							(ToOneAttributeSource) attributeSource,
//							context
//					);
//				}
//				case ANY: {
//					return buildAnyAttributeBinding(
//							container,
//							attributeSource,
//							context
//					);
//				}
//				default: {
//					throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
//							.makeMappingException(
//									"Unrecognized SingularAttributeNature : " +
//											attributeSource.getSingularAttributeNature()
//							);
//				}
//			}
//		}
//
//		@Override
//		public BasicAttributeBinding buildBasicAttributeBinding(
//				AttributeBindingContainer container,
//				SingularAttributeSource attributeSource,
//				BinderRootContext context) {
//			SingularAttribute attribute = container.getAttributeContainer()
//					.locateSingularAttribute( attributeSource.getName() );
//			if ( attribute == null ) {
//				attribute = createSingularAttribute( container, attributeSource );
//			}
//
//			final List<RelationalValueBinding> relationalValueBindings = determineRelationalValueBindings(
//					container,
//					attributeSource,
//					attribute,
//					locateDefaultTableSpecificationForAttribute( container, attributeSource ),
//					context
//			);
//			final BasicAttributeBinding attributeBinding = container.makeBasicAttributeBinding(
//					attribute,
//					relationalValueBindings,
//					determineAccessStrategyName( attributeSource, context ),
//					attributeSource.isIncludedInOptimisticLocking(),
//					attributeSource.isLazy(),
//					attributeSource.getNaturalIdMutability(),
//					createMetaAttributeContext( container, attributeSource ),
//					attributeSource.getAttributeRole(),
//					attributeSource.getAttributePath(),
//					attributeSource.getGeneration()
//			);
//			context.typeHelper().bindSingularAttributeType( attributeSource, attributeBinding );
//			return attributeBinding;
//		}
//
//		protected List<RelationalValueBinding> determineRelationalValueBindings(
//				AttributeBindingContainer container,
//				SingularAttributeSource attributeSource,
//				SingularAttribute attribute,
//				TableSpecification implicitTable,
//				BinderRootContext context) {
//			return context.relationalValueBindingHelper().createRelationalValueBindings(
//					container,
//					attributeSource,
//					attribute,
//					implicitTable,
//					false
//			);
//		}
//
//		protected String determineAccessStrategyName(AttributeSource attributeSource, BinderRootContext context) {
//			final String explicitStrategyName = attributeSource.getPropertyAccessorName();
//			if ( explicitStrategyName != null ) {
//				return explicitStrategyName;
//			}
//
//			return context.getMappingDefaults().getPropertyAccessorName();
//		}
//
//		@Override
//		public EmbeddedAttributeBinding buildCompositeAttributeBinding(
//				AttributeBindingContainer container,
//				EmbeddedAttributeSource attributeSource,
//				BinderRootContext context) {
//			final SingularAttribute attribute = locateOrCreateCompositeAttribute(
//					container,
//					attributeSource,
//					context
//			);
//
//			if ( attribute == null ) {
//				throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
//						.makeMappingException(
//								"Could not locate/build composite attribute '" + attributeSource.getName()
//						);
//			}
//
//			if ( !Aggregate.class.isInstance( attribute.getSingularAttributeType() ) ) {
//				throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
//						.makeMappingException(
//								"Found existing attribute on container for '" + attributeSource.getName()
//										+ "', but was expecting an Aggregate"
//						);
//			}
//
//			EmbeddedAttributeBinding attributeBinding = createAggregatedCompositeAttribute(
//					container,
//					attributeSource,
//					attribute,
//					context
//			);
//
//			bindAttributes( attributeBinding.getEmbeddableBinding(), attributeSource.getEmbeddableSource() );
//
//			context.typeHelper().bindAggregatedCompositeAttributeType(
//					context.getServiceRegistry(),
//					isBuildingIdentifierAttributes(),
//					(Aggregate) attributeBinding.getAttribute().getSingularAttributeType(),
//					// TODO: don't have the default value at this point; shouldn't be needed...
//					null,
//					attributeBinding
//			);
//			return attributeBinding;
//		}
//
//		protected SingularAttribute locateOrCreateCompositeAttribute(
//				AttributeBindingContainer attributeBindingContainer,
//				EmbeddedAttributeSource attributeSource,
//				BinderRootContext context) {
//			SingularAttribute attribute = attributeBindingContainer.getAttributeContainer()
//					.locateCompositeAttribute( attributeSource.getName() );
//
//			if ( attribute != null ) {
//				return attribute;
//			}
//
//			final JavaTypeDescriptor compositeTypeDescriptor;
//			if ( attributeSource.getEmbeddableSource().getTypeDescriptor() != null ) {
//				compositeTypeDescriptor = attributeSource.getEmbeddableSource().getTypeDescriptor();
//			}
//			else {
//				final EntityMode entityMode =
//						attributeBindingContainer.seekEntityBinding().getHierarchyDetails().getEntityMode();
//				compositeTypeDescriptor = context.typeHelper().determineJavaType(
//						attributeSource,
//						attributeBindingContainer.getAttributeContainer(),
//						entityMode
//				);
//			}
//
//			final Aggregate composite = new Aggregate( compositeTypeDescriptor, null );
//			return attributeBindingContainer.getAttributeContainer().createCompositeAttribute(
//					attributeSource.getName(),
//					composite
//			);
//		}
//
//		protected EmbeddedAttributeBinding createAggregatedCompositeAttribute(
//				AttributeBindingContainer attributeBindingContainer,
//				EmbeddedAttributeSource attributeSource,
//				SingularAttribute attribute,
//				BinderRootContext context) {
//			final Aggregate composite = (Aggregate) attribute.getSingularAttributeType();
//
//			final boolean hasParentReference = StringHelper.isNotEmpty(
//					attributeSource.getEmbeddableSource().getParentReferenceAttributeName()
//			);
//
//			final SingularAttribute referencingAttribute = hasParentReference
//					? null
//					: composite.createSingularAttribute( attributeSource.getEmbeddableSource().getParentReferenceAttributeName() );
//			final NaturalIdMutability naturalIdMutability = attributeSource.getNaturalIdMutability();
//
//			Class<? extends ComponentTuplizer> tuplizerClass = null;
//			if ( attributeSource.getEmbeddableSource().getExplicitTuplizerClassName() != null ) {
//				tuplizerClass = context.getServiceRegistry()
//						.getService( ClassLoaderService.class )
//						.classForName( attributeSource.getEmbeddableSource().getExplicitTuplizerClassName() );
//			}
//
//			return attributeBindingContainer.makeAggregatedCompositeAttributeBinding(
//					attribute,
//					referencingAttribute,
//					tuplizerClass,
//					determineAccessStrategyName( attributeSource, context ),
//					attributeSource.isIncludedInOptimisticLocking(),
//					attributeSource.isLazy(),
//					naturalIdMutability,
//					createMetaAttributeContext( attributeBindingContainer, attributeSource ),
//					attributeSource.getAttributeRole(),
//					attributeSource.getAttributePath()
//			);
//		}
//
//		protected boolean isBuildingIdentifierAttributes() {
//			return false;
//		}
//
//		@Override
//		public OneToOneAttributeBinding buildOneToOneAttributeBinding(
//				AttributeBindingContainer container,
//				ToOneAttributeSource attributeSource,
//				BinderRootContext context) {
//			SingularAttribute attribute = container.getAttributeContainer()
//					.locateSingularAttribute( attributeSource.getName() );
//			if ( attribute == null ) {
//				attribute = createSingularAttribute( container, attributeSource );
//			}
//
//			final JavaTypeDescriptor referencedEntityJavaClassReference = context.typeHelper().determineJavaType(
//					attributeSource,
//					container.getAttributeContainer()
//			);
//			final EntityBinding referencedEntityBinding = locateEntityBinding(
//					referencedEntityJavaClassReference,
//					attributeSource.getReferencedEntityName(),
//					context
//			);
//
//			final AssociationRelationalBindingResolver resolver =
//					getAssociationRelationalBindingResolver( attributeSource, context );
//
//			//now find the referenced attribute binding, either the referenced entity's id attribute or the referenced attribute
//			//todo referenced entityBinding null check?
//			final SingularAttributeBinding referencedAttributeBinding = resolver.resolveOneToOneReferencedAttributeBinding(
//					attributeSource,
//					referencedEntityBinding
//			);
//			// todo : currently a chicken-egg problem here between creating the attribute binding and binding its FK values...
//			// now we have everything to create the attribute binding
//			final OneToOneAttributeBinding attributeBinding = container.makeOneToOneAttributeBinding(
//					attribute,
//					determineAccessStrategyName( attributeSource, context ),
//					attributeSource.isIncludedInOptimisticLocking(),
//					attributeSource.isLazy(),
//					attributeSource.getNaturalIdMutability(),
//					createMetaAttributeContext( container, attributeSource ),
//					attributeSource.getAttributeRole(),
//					attributeSource.getAttributePath(),
//					referencedEntityBinding,
//					referencedAttributeBinding,
//					attributeSource.getForeignKeyDirection() == ForeignKeyDirection.FROM_PARENT
//			);
//
//			/**
//			 * this is not correct, here, if no @JoinColumn defined, we simply create the FK column only with column calucated
//			 * but what we should do is get all the column info from the referenced column(s), including nullable, size etc.
//			 */
//			final List<RelationalValueBinding> relationalValueBindings = resolver.resolveOneToOneRelationalValueBindings(
//					attributeSource,
//					container,
//					referencedAttributeBinding
//			);
//			final ForeignKey foreignKey;
//			if ( attributeSource.getForeignKeyDirection() == ForeignKeyDirection.FROM_PARENT ) {
//				foreignKey = resolver.resolveOneToOneForeignKey(
//						attributeSource,
//						attributeBinding.getContainer().getPrimaryTable(),
//						attributeBinding.getContainer().getPrimaryTable().getPrimaryKey().getColumns(),
//						referencedEntityBinding
//				);
//			}
//			else {
//				foreignKey = null;
//			}
//			attributeBinding.setJoinRelationalValueBindings( relationalValueBindings, foreignKey );
//
//			// Type resolution...
//			if ( !attribute.isTypeResolved() ) {
//				attribute.resolveType( referencedEntityBinding.getEntity() );
//			}
//
//
//			String uniqueKeyAttributeName = null;
//			final EntityIdentifier idInfo = referencedEntityBinding.getHierarchyDetails().getEntityIdentifier();
//			if ( idInfo.getNature() != EntityIdentifierNature.NON_AGGREGATED_COMPOSITE ) {
//				final EntityIdentifier.AttributeBasedIdentifierBinding idBinding =
//						(EntityIdentifier.AttributeBasedIdentifierBinding) idInfo.getEntityIdentifierBinding();
//				final SingularAttributeBinding idAttrBinding = idBinding.getAttributeBinding();
//				if ( referencedAttributeBinding != idAttrBinding ) {
//					referencedAttributeBinding.setAlternateUniqueKey( true );
//					uniqueKeyAttributeName = referencedAttributeBinding.getAttributePath().getFullPath();
//				}
//			}
//
//			final Type resolvedType;
//			if ( attributeSource.isMappedBy() || attributeSource.relationalValueSources().isEmpty() )  {
//				resolvedType = context.getMetadataCollector().getTypeResolver().getTypeFactory().oneToOne(
//						referencedEntityBinding.getEntityName(),
//						attributeSource.getForeignKeyDirection(),
//						uniqueKeyAttributeName == null,
//						uniqueKeyAttributeName,
//						attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
//						attributeSource.isUnWrapProxy(),
//						container.seekEntityBinding().getEntityName(),
//						attribute.getName()
//				);
//			}
//			else {
//				resolvedType = context.getMetadataCollector().getTypeResolver().getTypeFactory().specialOneToOne(
//						referencedEntityBinding.getEntityName(),
//						attributeSource.getForeignKeyDirection(),
//						uniqueKeyAttributeName == null,
//						uniqueKeyAttributeName,
//						attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
//						attributeSource.isUnWrapProxy(),
//						container.seekEntityBinding().getEntityName(),
//						attribute.getName()
//				);
//			}
//
//			context.typeHelper().bindHibernateTypeDescriptor(
//					attributeBinding.getHibernateTypeDescriptor(),
//					attributeSource.getTypeInformation(),
//					referencedEntityJavaClassReference,
//					resolvedType
//			);
//			if ( !attributeBinding.getRelationalValueBindings().isEmpty() ) {
//				context.typeHelper().bindJdbcDataType(
//						attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
//						attributeBinding.getRelationalValueBindings()
//				);
//			}
//
//			bindToOneDetails( attributeSource, attributeBinding );
//
//			return attributeBinding;
//		}
//
//		protected EntityBinding locateEntityBinding(
//				JavaTypeDescriptor referencedEntityTypeDescriptor,
//				String explicitEntityName,
//				BinderRootContext context) {
//			if ( explicitEntityName != null ) {
//				return locateEntityBinding( explicitEntityName, context );
//			}
//			else if ( referencedEntityTypeDescriptor != null ) {
//				return locateEntityBinding( referencedEntityTypeDescriptor.getName().toString(), context );
//			}
//
//			throw new IllegalArgumentException( "explicitEntityName and entityJavaClassReference cannot both be null." );
//		}
//
//		protected EntityBinding locateEntityBinding(String entityName, BinderRootContext context) {
//			// Check if binding has already been created
//			EntityBinding entityBinding = context.getMetadataCollector().getEntityBinding( entityName );
//			if ( entityBinding == null ) {
//				throw context.getLocalBindingContextSelector()
//						.getCurrentBinderLocalBindingContext()
//						.makeMappingException( "No entity binding with name: " + entityName );
//			}
//			return entityBinding;
//		}
//
//		protected AssociationRelationalBindingResolver getAssociationRelationalBindingResolver(
//				ToOneAttributeSource attributeSource,
//				BinderRootContext context) {
//			return attributeSource.isMappedBy()
//					? mappedByAssociationRelationalBindingResolver( context )
//					: standardAssociationRelationalBindingResolver( context );
//		}
//
//		private AssociationRelationalBindingResolver mappedByAssociationRelationalBindingResolver(BinderRootContext context) {
//			if ( mappedByAssociationRelationalBindingResolver == null ) {
//				mappedByAssociationRelationalBindingResolver =
//						new MappedByAssociationRelationalBindingResolverImpl( context );
//			}
//			return mappedByAssociationRelationalBindingResolver;
//		}
//
//		private AssociationRelationalBindingResolver standardAssociationRelationalBindingResolver(BinderRootContext context) {
//			if ( standardAssociationRelationalBindingResolver == null ) {
//				standardAssociationRelationalBindingResolver =
//						new StandardAssociationRelationalBindingResolverImpl( context );
//			}
//			return standardAssociationRelationalBindingResolver;
//		}
//
//		protected void bindToOneDetails(
//				final ToOneAttributeSource attributeSource,
//				final SingularAssociationAttributeBinding attributeBinding) {
//			attributeBinding.setCascadeStyle( determineCascadeStyle( attributeSource.getCascadeStyles() ) );
//			attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
//			attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );
//		}
//
//		@Override
//		public ManyToOneAttributeBinding buildManyToOneAttributeBinding(
//				AttributeBindingContainer container,
//				ToOneAttributeSource attributeSource,
//				BinderRootContext context) {
//			return null;
//		}
//
//		@Override
//		public SingularAttributeBinding buildAnyAttributeBinding(
//				AttributeBindingContainer container,
//				SingularAttributeSource attributeSource,
//				BinderRootContext context) {
//			throw new NotYetImplementedException( "Binding ANY mappings not yet implemented" );
//		}
//
//		@Override
//		public PluralAttributeBinding buildPluralAttributeBinding(
//				AttributeBindingContainer container,
//				PluralAttributeSource attributeSource,
//				BinderRootContext context) {
//			return null;
//		}
//
//		@Override
//		public PluralAttributeBinding buildManyToAnyAttributeBinding(
//				AttributeBindingContainer container,
//				PluralAttributeSource attributeSource,
//				BinderRootContext context) {
//			return null;
//		}
//	}
//
//	public static class PluralAttributesDisallowedAttributeBuilder extends StandardAttributeBindingBuilder {
//		/**
//		 * Singleton access
//		 */
//		public static final PluralAttributesDisallowedAttributeBuilder INSTANCE = new PluralAttributesDisallowedAttributeBuilder();
//
//		@Override
//		public PluralAttributeBinding buildPluralAttributeBinding(
//				AttributeBindingContainer container,
//				PluralAttributeSource attributeSource,
//				BinderRootContext context) {
//			// sources should already disallow this, this is simply a
//			// last-stand, fail-safe check so we use a generic exception message
//			throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
//					.makeMappingException( "Plural attributes not allowed in this context" );
//		}
//
//		@Override
//		public PluralAttributeBinding buildManyToAnyAttributeBinding(
//				AttributeBindingContainer container,
//				PluralAttributeSource attributeSource,
//				BinderRootContext context) {
//			// sources should already disallow this, this is simply a
//			// last-stand, fail-safe check so we use a generic exception message
//			throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
//					.makeMappingException( "Plural attributes not allowed in this context" );
//		}
//
//	}
//
//	public static class IdentifierAttributeBuilder extends StandardAttributeBindingBuilder {
//		/**
//		 * Singleton access
//		 */
//		public static final IdentifierAttributeBuilder INSTANCE = new IdentifierAttributeBuilder();
//
//		@Override
//		public PluralAttributeBinding buildPluralAttributeBinding(
//				AttributeBindingContainer container,
//				PluralAttributeSource attributeSource,
//				BinderRootContext context) {
//			// sources should already disallow this, this is simply a
//			// last-stand, fail-safe check so we use a generic exception message
//			throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
//					.makeMappingException( "Plural attributes not allowed in identifier" );
//		}
//
//		@Override
//		public PluralAttributeBinding buildManyToAnyAttributeBinding(
//				AttributeBindingContainer container,
//				PluralAttributeSource attributeSource,
//				BinderRootContext context) {
//			// sources should already disallow this, this is simply a
//			// last-stand, fail-safe check so we use a generic exception message
//			throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
//					.makeMappingException( "Plural attributes not allowed in identifier" );
//		}
//
//		@Override
//		public SingularAttributeBinding buildAnyAttributeBinding(
//				AttributeBindingContainer container,
//				SingularAttributeSource attributeSource,
//				BinderRootContext context) {
//			// sources should already disallow this, this is simply a
//			// last-stand, fail-safe check so we use a generic exception message
//			throw context.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext()
//					.makeMappingException( "Any mapping not allowed in identifier" );
//		}
//	}























}
