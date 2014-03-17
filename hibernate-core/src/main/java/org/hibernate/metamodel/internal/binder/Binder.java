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
import java.util.LinkedHashMap;
import java.util.List;
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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.resolver.AssociationRelationalBindingResolver;
import org.hibernate.metamodel.internal.resolver.MappedByAssociationRelationalBindingResolverImpl;
import org.hibernate.metamodel.internal.resolver.StandardAssociationRelationalBindingResolverImpl;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.source.spi.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.source.spi.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.source.spi.BasicPluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.ComponentAttributeSource;
import org.hibernate.metamodel.source.spi.CompositePluralAttributeElementSource;
import org.hibernate.metamodel.source.spi.CompositePluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.ConstraintSource;
import org.hibernate.metamodel.source.spi.DerivedValueSource;
import org.hibernate.metamodel.source.spi.DiscriminatorSource;
import org.hibernate.metamodel.source.spi.EntityAttributePluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.FilterSource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.IndexConstraintSource;
import org.hibernate.metamodel.source.spi.IndexedPluralAttributeSource;
import org.hibernate.metamodel.source.spi.JoinedSubclassEntitySource;
import org.hibernate.metamodel.source.spi.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.source.spi.MappedByAssociationSource;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.source.spi.MultiTenancySource;
import org.hibernate.metamodel.source.spi.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.OneToManyPluralAttributeElementSource;
import org.hibernate.metamodel.source.spi.Orderable;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSource;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.RelationalValueSourceContainer;
import org.hibernate.metamodel.source.spi.SecondaryTableSource;
import org.hibernate.metamodel.source.spi.SequentialPluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.SimpleIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.Sortable;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.source.spi.UniqueConstraintSource;
import org.hibernate.metamodel.source.spi.VersionAttributeSource;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.Cascadeable;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.CompositePluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.CompositePluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.EntityVersion;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.HierarchyDetails;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.ManyToManyPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.binding.OneToManyPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.OneToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SetBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.BasicType;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.IdentifiableType;
import org.hibernate.metamodel.spi.domain.IndexedPluralAttribute;
import org.hibernate.metamodel.spi.domain.MappedSuperclass;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;
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

import static org.hibernate.MultiTenancyStrategy.DISCRIMINATOR;
import static org.hibernate.engine.spi.SyntheticAttributeHelper.SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME;
import static org.hibernate.metamodel.internal.binder.HibernateTypeHelper.ReflectedCollectionJavaTypes;

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
	// Entity hierarchies and source index need be available throughout the binding process
	private final Map<String, EntityHierarchySource> entityHierarchiesByRootEntityName =
			new LinkedHashMap<String, EntityHierarchySource>();
	private final SourceIndex sourceIndex = new SourceIndex();

	private final BinderRootContextImpl rootBindingContext;
	private final BinderProcessHelper processHelper;

	private final JavaTypeDescriptorRepository javaTypeDescriptorRepository;

	// todo : hook this map in with the context method to make domain types...
	private final Map<JavaTypeDescriptor, org.hibernate.metamodel.spi.domain.Type> domainModelTypes
			= new HashMap<JavaTypeDescriptor, org.hibernate.metamodel.spi.domain.Type>();

	private final StandardAssociationRelationalBindingResolverImpl standardAssociationRelationalBindingResolver;
	private final MappedByAssociationRelationalBindingResolverImpl mappedByAssociationRelationalBindingResolver;

	private final Map<String,CacheRegionDefinition> entityCacheRegionDefinitionsByEntityName = new HashMap<String, CacheRegionDefinition>();

	private final Map<String,Collection<String>> identifierDependencyMap = new HashMap<String, Collection<String>>();


	public Binder(BindingContext rootBindingContext) {
		this.rootBindingContext = new BinderRootContextImpl( rootBindingContext );

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
			entityHierarchiesByRootEntityName.put(
					source.getRoot().getEntityName(),
					source
			);
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
						localBindingContext().<EntityTuplizer>locateClassByName(
								customTuplizerClassName
						)
				);
			}
			final String customPersisterClassName = source.getCustomPersisterClassName();
			if ( customPersisterClassName != null ) {
				binding.setCustomEntityPersisterClass(
						localBindingContext().<EntityPersister>locateClassByName(
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

		final DotName descriptorTypeName = javaTypeDescriptorRepository.buildName( source.getTypeName() );
		final JavaTypeDescriptor javaTypeDescriptor = javaTypeDescriptorRepository.getType( descriptorTypeName );

		if ( EntitySource.class.isInstance( source ) ) {
			Entity entityDomainType = (Entity) domainModelTypes.get( javaTypeDescriptor );

			if ( entityDomainType == null ) {
				entityDomainType = new Entity(
						javaTypeDescriptor,
						resolveDomainType( source.getSuperType() )
				);
				domainModelTypes.put( javaTypeDescriptor, entityDomainType );
			}

			return entityDomainType;
		}
		else {
			MappedSuperclass superDomainType = (MappedSuperclass) domainModelTypes.get( javaTypeDescriptor );

			if ( superDomainType == null ) {
				superDomainType = new MappedSuperclass(
						javaTypeDescriptor,
						resolveDomainType( source.getSuperType() )
				);
				domainModelTypes.put( javaTypeDescriptor, superDomainType );
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

		// Bind all composite attribute containers. This excludes composite sub-attributes.
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( false, SingularAttributeSource.Nature.COMPOSITE ) );

		// bind basic singular attributes, including composite sub-attributes that are basic.
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( false, SingularAttributeSource.Nature.BASIC ) );

		// many-to-one needs to be bound before one-to-one (um, can't remember why).

		// bind non-mappedby many-to-one and one-to-one attributes, including composite sub-attributes that are many-to-one/one-to-one.
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( false, SingularAttributeSource.Nature.MANY_TO_ONE ) );
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( false, SingularAttributeSource.Nature.ONE_TO_ONE ) );

		// bind mappedby many-to-one and one-to-one attributes, including composite sub-attributes that are many-to-one/one-to-one.
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( true, SingularAttributeSource.Nature.MANY_TO_ONE ) );
		applyToAllEntityHierarchies( bindSingularAttributesExecutor( true, SingularAttributeSource.Nature.ONE_TO_ONE ) );

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
		processHelper.apply( entityHierarchiesByRootEntityName.values(), strategy );
	}

	private void applyToAllEntityHierarchies(BinderStepEntityStrategy strategy) {
		processHelper.apply( entityHierarchiesByRootEntityName.values(), strategy );
	}

	private void applyToAllEntityHierarchies(BinderStepCombinedStrategy strategy) {
		processHelper.apply( entityHierarchiesByRootEntityName.values(), strategy );
	}

	private void applyToAllEntityHierarchies(
			BinderStepHierarchyStrategy hierarchyStrategy,
			BinderStepEntityStrategy entityStrategy) {
		processHelper.apply( entityHierarchiesByRootEntityName.values(), hierarchyStrategy, entityStrategy );
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
		final BinderStepCombinedStrategy strategy = new BinderStepCombinedStrategy() {
			@Override
			public void visit(EntityHierarchySource source, BinderLocalBindingContext context) {
				final HierarchyDetails binding = context.locateBinding( source );
				bindIdentifier( binding, source );
				bindSecondaryTables( binding.getRootEntityBinding(), source.getRoot() );
			}

			@Override
			public boolean applyToRootEntity() {
				return false;
			}

			@Override
			public void visit(EntitySource source, BinderLocalBindingContext context) {
				final EntityBinding binding = context.locateBinding( source );
				bindSubEntityPrimaryKey( binding, source );
				bindSecondaryTables( binding, source );
				markSuperEntityTableAbstractIfNecessary( binding.getSuperEntityBinding() );
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
							secondaryTableSource.isCascadeDeleteEnabled()
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

			private void bindIdentifier(HierarchyDetails binding, EntityHierarchySource source) {
				final IdentifierSource identifierSource = source.getIdentifierSource();
				final EntityIdentifierNature nature = identifierSource.getNature();

				switch ( nature ) {
					case SIMPLE: {
						bindSimpleIdentifier(
								binding,
								(SimpleIdentifierSource) identifierSource
						);
						break;
					}
					case AGGREGATED_COMPOSITE: {
						bindAggregatedCompositeIdentifier(
								binding,
								(AggregatedCompositeIdentifierSource) identifierSource
						);
						break;
					}
					case NON_AGGREGATED_COMPOSITE: {
						bindNonAggregatedCompositeIdentifier(
								binding,
								(NonAggregatedCompositeIdentifierSource) identifierSource
						);
						break;
					}
					default: {
						throw localBindingContext().makeMappingException( "Unknown identifier nature : " + nature.name() );
					}
				}
			}

			private void bindSimpleIdentifier(
					final HierarchyDetails binding,
					final SimpleIdentifierSource identifierSource) {
				// locate the attribute binding
				final SingularAttributeBinding idAttributeBinding = (SingularAttributeBinding) bindIdentifierAttribute(
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
						unsavedValue,
						identifierSource.getLookupIdClass(),
						propertyAccessorName( identifierSource.getIdClassPropertyAccessorName() )
				);

			}

			private  String interpretIdentifierUnsavedValue(
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

			private void bindAggregatedCompositeIdentifier(
					final HierarchyDetails binding,
					final AggregatedCompositeIdentifierSource identifierSource) {
				// locate the attribute binding
				final CompositeAttributeBinding idAttributeBinding = (CompositeAttributeBinding) bindIdentifierAttribute(
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

				binding.getEntityIdentifier().prepareAsAggregatedCompositeIdentifier(
						idAttributeBinding,
						generator,
						unsavedValue,
						identifierSource.getLookupIdClass(),
						propertyAccessorName( identifierSource.getIdClassPropertyAccessorName() )
				);
			}

			private void bindNonAggregatedCompositeIdentifier(
					final HierarchyDetails binding,
					final NonAggregatedCompositeIdentifierSource identifierSource) {
				// locate the attribute bindings for the real attributes
				final List<SingularAttributeBinding> idAttributeBindings = new ArrayList<SingularAttributeBinding>();
				for ( SingularAttributeSource attributeSource : identifierSource.getAttributeSourcesMakingUpIdentifier() ) {
					final SingularAttributeBinding singularAttributeBinding = bindIdentifierAttribute(
							binding.getRootEntityBinding(),
							attributeSource
					);
					idAttributeBindings.add( singularAttributeBinding );
				}

				final Class idClassType = identifierSource.getLookupIdClass();
				final String idClassPropertyAccessorName = idClassType == null
						? null
						: propertyAccessorName( identifierSource.getIdClassPropertyAccessorName() );

				// Configure ID generator
				IdentifierGeneratorDefinition generator = identifierSource.getIdentifierGeneratorDescriptor();
				if ( generator == null ) {
					final Map<String, String> params = new HashMap<String, String>();
					params.put( IdentifierGenerator.ENTITY_NAME, binding.getRootEntityBinding().getEntityName() );
					generator = new IdentifierGeneratorDefinition( "default_assign_identity_generator", "assigned", params );
				}
				// Create the synthetic attribute
				final SingularAttribute syntheticAttribute = binding.getRootEntityBinding().getEntity().createSyntheticCompositeAttribute(
						SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME,
						binding.getRootEntityBinding().getEntity()
				);

				final CompositeAttributeBinding syntheticAttributeBinding = binding.getRootEntityBinding()
						.makeVirtualCompositeAttributeBinding(
								syntheticAttribute,
								createMetaAttributeContext(
										binding.getRootEntityBinding(),
										identifierSource.getToolingHintSources()
								),
								idAttributeBindings
						);

				// Create the synthetic attribute binding.
				binding.getEntityIdentifier().prepareAsNonAggregatedCompositeIdentifier(
						syntheticAttributeBinding,
						generator,
						interpretIdentifierUnsavedValue( identifierSource, generator ),
						idClassType,
						idClassPropertyAccessorName
				);

				typeHelper().bindNonAggregatedCompositeIdentifierType(
						localBindingContext().getServiceRegistry(),
						syntheticAttributeBinding,
						syntheticAttribute
				);
			}

			private void bindSubEntityPrimaryKey(EntityBinding entityBinding, EntitySource entitySource) {
				final InheritanceType inheritanceType = entityBinding.getHierarchyDetails().getInheritanceType();
				final EntityBinding superEntityBinding = entityBinding.getSuperEntityBinding();
				if ( superEntityBinding == null ) {
					throw new AssertionFailure( "super entitybinding is null " );
				}
				if ( inheritanceType == InheritanceType.JOINED ) {
					JoinedSubclassEntitySource subclassEntitySource = (JoinedSubclassEntitySource) entitySource;
					final List<ColumnSource> columnSources = subclassEntitySource.getPrimaryKeyColumnSources();
					final TableSpecification table = entityBinding.getPrimaryTable();
					final List<RelationalValueBinding> joinRelationalValueBindings =
							getJoinedPrimaryKeyRelationalValueBindings( superEntityBinding, columnSources, table );

					entityBinding.setKeyRelationalValueBindings( joinRelationalValueBindings );
					List<Column> targetColumns =
							foreignKeyHelper().determineForeignKeyTargetColumns(
									superEntityBinding,
									subclassEntitySource
							);

					ForeignKey foreignKey = locateOrCreateForeignKey(
							subclassEntitySource.getExplicitForeignKeyName(),
							table,
							joinRelationalValueBindings,
							foreignKeyHelper().determineForeignKeyTargetTable(
									superEntityBinding,
									subclassEntitySource
							),
							targetColumns,
							subclassEntitySource.isCascadeDeleteEnabled()
					);

					if ( subclassEntitySource.isCascadeDeleteEnabled() ) {
						entityBinding.setCascadeDeleteEnabled( true );
					}
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
		};

		// TODO: need to determine the proper order for processing EntityHierarchySource objects
		//       so that dependent EntityHierarchySource is processed after the EntityHierarchySource it
		//       is dependent on.
		// For now, just delay processing the dependent entity hierarchies.
		final Set<EntityHierarchySource> unresolvedEntityHierarchies = new HashSet<EntityHierarchySource>();
		for ( final EntityHierarchySource entityHierarchySource : entityHierarchiesByRootEntityName.values() ) {
			if ( isIdentifierDependentOnOtherEntityHierarchy( entityHierarchySource ) ) {
				unresolvedEntityHierarchies.add( entityHierarchySource );
			}
			else {
				processHelper.apply( entityHierarchySource, strategy );
			}
		}

		// The following is to try to resolve any dependent entity hierarchies.
		// It runs through all the dependent entity hierarchies and resolves what it can.
		// This process repeats until no more can be resolved.
		// TODO: this will not be necessary once we know the proper order for
		//       processing entity hierarchies.
		int oldSize = Integer.MAX_VALUE;
		while( !unresolvedEntityHierarchies.isEmpty() && unresolvedEntityHierarchies.size() < oldSize ) {
			oldSize = unresolvedEntityHierarchies.size();
			for ( Iterator<EntityHierarchySource> it = unresolvedEntityHierarchies.iterator(); it.hasNext(); ) {
				final EntityHierarchySource entityHierarchySource = it.next();
				try {
					processHelper.apply( entityHierarchySource, strategy );
					// succeeded, so the entityHierarchySource is no longer unresolved.
					it.remove();
				}
				catch (Exception ex) {
					// to nothing;
				}
			}
		}
		// If any entity hierarchies cannot be resolved, then throw exception.
		if ( ! unresolvedEntityHierarchies.isEmpty() ) {
			throw new IllegalStateException( "could not resolve all EntityHierarchies." );
		}
	}

	// TODO: this will not be necessary once we know the proper order for
	//       processing entity hierarchies.
	private boolean isIdentifierDependentOnOtherEntityHierarchy(EntityHierarchySource entityHierarchySource) {
		final EntitySource rootEntitySource = entityHierarchySource.getRoot();
		final IdentifierSource identifierSource = entityHierarchySource.getIdentifierSource();
		if ( identifierSource.getNature() != EntityIdentifierNature.SIMPLE ) {
			final List<? extends AttributeSource> subAttributeSources;
			if ( identifierSource.getNature() == EntityIdentifierNature.AGGREGATED_COMPOSITE ) {
				subAttributeSources = ( (AggregatedCompositeIdentifierSource) identifierSource ).getIdentifierAttributeSource().attributeSources();
			}
			else {
				subAttributeSources = ( (NonAggregatedCompositeIdentifierSource) identifierSource ).getAttributeSourcesMakingUpIdentifier();
			}
			return containsSingularAssociation( subAttributeSources );
		}
		else {
			final SimpleIdentifierSource simpleIdentifierSource = (SimpleIdentifierSource) identifierSource;
			return simpleIdentifierSource.getIdentifierAttributeSource().getNature() != SingularAttributeSource.Nature.BASIC;
		}
	}

	// TODO: this will not be necessary once we know the proper order for
	//       processing entity hierarchies.
	private boolean containsSingularAssociation(List<? extends AttributeSource> subAttributeSources) {
		for ( AttributeSource attributeSource : subAttributeSources ) {
			SingularAttributeSource singularAttributeSource = (SingularAttributeSource) attributeSource;
			// If the attribute source is for a -to-one association, the nature may not be resolved yet.
			if ( ToOneAttributeSource.class.isInstance(  singularAttributeSource ) ) {
				return true;
			}
			else if ( ( (SingularAttributeSource) attributeSource ).getNature() == SingularAttributeSource.Nature.COMPOSITE ) {
				ComponentAttributeSource componentAttributeSource = (ComponentAttributeSource) attributeSource;
				return containsSingularAssociation( componentAttributeSource.attributeSources() );
			}
		}
		return false;
	}

	private AttributeBindingContainer locateAttributeBindingContainer(final EntityBinding entityBinding, final String containerPath) {
		return StringHelper.isEmpty( containerPath )
				? entityBinding
				: (AttributeBindingContainer) entityBinding.locateAttributeBindingByPath( containerPath, false );
	}

	private BinderStepEntityStrategy bindSingularAttributesExecutor(
			final boolean isMappedBy,
			final SingularAttributeSource.Nature nature) {
		return new BinderStepEntityStrategy() {
			@Override
			public boolean applyToRootEntity() {
				return true;
			}

			@Override
			public void visit(EntitySource source, BinderLocalBindingContext context) {
				final EntityBinding binding = context.locateBinding( source );
				bindSingularAttributes( binding, isMappedBy, nature );

			}
		};
	}

	// TODO: create separate methods that are more clear for the cases.
	private void bindSingularAttributes(
			final EntityBinding entityBinding,
			final boolean isMappedBy,
			final SingularAttributeSource.Nature nature) {
		// Get the map of all attributes for the entity binding of the specified nature.
		Map<SourceIndex.AttributeSourceKey, SingularAttributeSource> map = sourceIndex.getSingularAttributeSources(
				entityBinding.getEntityName(),
				isMappedBy,
				nature
		);
		for ( Map.Entry<SourceIndex.AttributeSourceKey, SingularAttributeSource> entry : map.entrySet() ){
			final SourceIndex.AttributeSourceKey attributeSourceKey = entry.getKey();
			final SingularAttributeSource attributeSource = entry.getValue();
			final AttributeBindingContainer attributeBindingContainer =
					locateAttributeBindingContainer( entityBinding, attributeSourceKey.containerPath() );
			if (  isMappedBy ) {
				if ( !ToOneAttributeSource.class.isInstance( attributeSource ) ) {
					throw new AssertionFailure(
							String.format(
									"mappedBy is true, but attributeSouce is not an association: %s",
									attributeSourceKey
							)
					);
				}
				bindMappedBySecondaryTableIfNecessary( entityBinding, (ToOneAttributeSource) attributeSource );
			}

			if ( nature == SingularAttributeSource.Nature.COMPOSITE ) {
				// This only creates the composite attribute container.
				createAggregatedCompositeAttribute(
						attributeBindingContainer,
						(ComponentAttributeSource) attributeSource,
						null
				);
			}
			else if ( attributeBindingContainer instanceof CompositeAttributeBinding ) {
				// This attribute source is within a composite; skip binding if it is the parent.
				final CompositeAttributeBinding compositeAttributeBinding = (CompositeAttributeBinding) attributeBindingContainer;
				final ComponentAttributeSource compositeAttributeSource =
						(ComponentAttributeSource) sourceIndex.attributeSource( entityBinding, compositeAttributeBinding );
				final SingularAttribute parentReference = compositeAttributeBinding.getParentReference();
				if ( parentReference == null || !parentReference.getName().equals( attributeSource.getName() ) ) {
					bindAttribute( attributeBindingContainer, attributeSource );
					completeCompositeAttributeBindingIfPossible(
							compositeAttributeBinding,
							compositeAttributeSource
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
			CompositeAttributeBinding compositeAttributeBinding,
			ComponentAttributeSource compositeAttributeSource) {
		// Find out the number of sub-attributes, excluding the parent attribute.
		final int nAttributeSourcesExcludingParent =
				compositeAttributeBinding.getParentReference() != null
						? compositeAttributeSource.attributeSources().size() - 1
						: compositeAttributeSource.attributeSources().size();
		if ( compositeAttributeBinding.attributeBindingSpan() == nAttributeSourcesExcludingParent ) {
			// All sub-attribute bindings are present; now check if all sub-attributes have
			// their type resolved.
			boolean allResolved = true;
			for ( AttributeBinding attributeBinding : compositeAttributeBinding.attributeBindings() ) {
				if ( attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() == null ) {
					// Something is not resolved.
					allResolved = false;
					break;
				}
			}
			if ( allResolved ) {
				// All are resolved, so we can bind the type.
				typeHelper().bindAggregatedCompositeAttributeType(
						localBindingContext().getServiceRegistry(),
						false,
						(Aggregate) compositeAttributeBinding.getAttribute().getSingularAttributeType(),
						null, // TODO: don't have the default value at this point; shouldn't be needed...
						compositeAttributeBinding
				);
				// Now check the container.
				if ( compositeAttributeBinding.getContainer() instanceof CompositeAttributeBindingContainer ) {
					// The container is also a CompositeAttributeBindingContainer.
					// We need this process for the container.
					final CompositeAttributeBinding parentCompositeAttributeBinding =
							(CompositeAttributeBinding) compositeAttributeBinding.seekEntityBinding().locateAttributeBinding(
									( compositeAttributeBinding.getContainer() ).getPathBase()
							);
					final ComponentAttributeSource parentCompositeAttributeSource =
							(ComponentAttributeSource) sourceIndex.attributeSource(
									parentCompositeAttributeBinding.seekEntityBinding().getEntityName(),
									parentCompositeAttributeBinding.getPathBase()
							);
					completeCompositeAttributeBindingIfPossible(
							parentCompositeAttributeBinding,
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
				Map<SourceIndex.AttributeSourceKey, PluralAttributeSource> map = sourceIndex.getPluralAttributeSources(
						entityBinding.getEntityName(),
						isInverse
				);
				for ( Map.Entry<SourceIndex.AttributeSourceKey, PluralAttributeSource> entry : map.entrySet() ){
					final SourceIndex.AttributeSourceKey attributeSourceKey = entry.getKey();
					final PluralAttributeSource attributeSource = entry.getValue();
					// Bind the attribute into the appropriate container.
					final AttributeBindingContainer attributeBindingContainer =
							locateAttributeBindingContainer( entityBinding, attributeSourceKey.containerPath() );
					bindAttribute( attributeBindingContainer, attributeSource );
					if ( attributeBindingContainer instanceof CompositeAttributeBinding ) {
						// We just bound a sub-attribute into a CompositeAttributeBinding.
						final CompositeAttributeBinding compositeAttributeBinding = (CompositeAttributeBinding) attributeBindingContainer;
						final ComponentAttributeSource compositeAttributeSource =
								(ComponentAttributeSource) sourceIndex.attributeSource(
										entityBinding.getEntityName(),
										compositeAttributeBinding.getPathBase()
								);
						// Resolve the type if types are resolved for all sub-attributes now.
						completeCompositeAttributeBindingIfPossible( compositeAttributeBinding, compositeAttributeSource );
					}
				}
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
		};
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Entity binding relates methods

	private EntityBinding locateEntityBinding(
			final JavaTypeDescriptor referencedEntityTypeDescriptor,
			final String explicitEntityName) {
		final String referencedEntityName;
		if ( explicitEntityName != null ) {
			referencedEntityName = explicitEntityName;
		}
		else if ( referencedEntityTypeDescriptor != null ) {
			referencedEntityName = referencedEntityTypeDescriptor.getName().toString();
		}
		else {
			throw new IllegalArgumentException( "explicitEntityName and entityJavaClassReference cannot both be null." );
		}
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
				mappedByAssociationSource.getReferencedEntityName(), mappedByAssociationSource.getMappedBy()
		);
		if ( ownerAttributeSource.isMappedBy() ) {
			throw new AssertionFailure( "owner attribute source has mappedBy != null" );
		}

		final EntityBinding ownerEntityBinding = locateEntityBinding( attributeSource.getReferencedEntityName() );

		if ( ownerAttributeSource.getNature() == SingularAttributeSource.Nature.ONE_TO_ONE ||
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
				attributeSource.isCascadeDeleteEnabled()
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
			joinRelationalValueBindings = new ArrayList<RelationalValueBinding>(
					joinedPrimaryKeyColumnSources
							.size()
			);
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
		typeHelper().bindJdbcDataType(
				entityBinding.getHierarchyDetails()
						.getEntityIdentifier()
						.getAttributeBinding()
						.getHibernateTypeDescriptor()
						.getResolvedTypeMapping(),
				joinRelationalValueBindings
		);
		return joinRelationalValueBindings;
	}

	public ForeignKey locateOrCreateForeignKey(
			final String foreignKeyName,
			final TableSpecification sourceTable,
			final List<RelationalValueBinding> sourceRelationalValueBindings,
			final TableSpecification targetTable,
			final List<Column> targetColumns,
			boolean isCascadeDeleteEnabled) {
		return foreignKeyHelper().locateOrCreateForeignKey(
				foreignKeyName,
				sourceTable,
				extractColumnsFromRelationalValueBindings( sourceRelationalValueBindings ),
				targetTable,
				targetColumns,
				isCascadeDeleteEnabled
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
			final CompositeAttributeBindingContainer compositeAttributeBindingContainer,
			final AttributeSourceContainer attributeSourceContainer) {
		if ( compositeAttributeBindingContainer.getParentReference() == null ) {
			bindAttributes(
					(AttributeBindingContainer) compositeAttributeBindingContainer,
					attributeSourceContainer
			);
		}
		else {
			for ( final AttributeSource subAttributeSource : attributeSourceContainer.attributeSources() ) {
				if ( !subAttributeSource.getName()
						.equals( compositeAttributeBindingContainer.getParentReference().getName() ) ) {
					bindAttribute(
							compositeAttributeBindingContainer,
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
		final List<RelationalValueBinding> relationalValueBindings =
				relationalValueBindingHelper().createRelationalValueBindings(
						attributeBindingContainer,
						attributeSource,
						attribute,
						locateDefaultTableSpecificationForAttribute( attributeBindingContainer, attributeSource ),
						false
				);
		final BasicAttributeBinding attributeBinding =
				attributeBindingContainer.makeBasicAttributeBinding(
						attribute,
						relationalValueBindings,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						attributeSource.getNaturalIdMutability(),
						createMetaAttributeContext( attributeBindingContainer, attributeSource ),
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
		final SingularAttributeSource.Nature nature = attributeSource.getNature();
		final SingularAttribute attribute =
				attributeBindingContainer.getAttributeContainer().locateSingularAttribute( attributeSource.getName() );
		switch ( nature ) {
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
						ComponentAttributeSource.class.cast( attributeSource ),
						attribute,
						isIdentifierAttribute
				);
			default:
				throw new NotYetImplementedException( nature.toString() );
		}
	}

	private CompositeAttributeBinding bindAggregatedCompositeAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ComponentAttributeSource attributeSource,
			SingularAttribute attribute,
			boolean isAttributeIdentifier) {
		CompositeAttributeBinding attributeBinding = createAggregatedCompositeAttribute(
				attributeBindingContainer, attributeSource, attribute
		);
		bindAttributes( attributeBinding, attributeSource );
		typeHelper().bindAggregatedCompositeAttributeType(
				localBindingContext().getServiceRegistry(),
				isAttributeIdentifier,
				(Aggregate) attributeBinding.getAttribute().getSingularAttributeType(),
				null, // TODO: don't have the default value at this point; shouldn't be needed...
				attributeBinding
		);
		return attributeBinding;
	}

	private CompositeAttributeBinding createAggregatedCompositeAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ComponentAttributeSource attributeSource,
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
					localBindingContext().makeMappingException(
							"Found existing attribute on container for '" + attributeSource.getName()
									+ "', but was expecting an Aggregate"
					);
				}
				composite = (Aggregate) attribute.getSingularAttributeType();
			}
			else {
				final JavaTypeDescriptor compositeTypeDescriptor;
				if ( attributeSource.getTypeDescriptor() != null ) {
					compositeTypeDescriptor = attributeSource.getTypeDescriptor();
				}
				else {
					compositeTypeDescriptor = typeHelper().determineJavaType(
							attributeSource,
							attributeBindingContainer.getAttributeContainer()
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

		final SingularAttribute referencingAttribute = StringHelper.isEmpty( attributeSource.getParentReferenceAttributeName() )
				? null
				: composite.createSingularAttribute( attributeSource.getParentReferenceAttributeName() );
		final NaturalIdMutability naturalIdMutability = attributeSource.getNaturalIdMutability();
		final CompositeAttributeBinding attributeBinding =
				attributeBindingContainer.makeAggregatedCompositeAttributeBinding(
						attribute,
						referencingAttribute,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						naturalIdMutability,
						createMetaAttributeContext( attributeBindingContainer, attributeSource )
				);
		if ( attributeSource.getExplicitTuplizerClassName() != null ) {
			Class<? extends ComponentTuplizer> tuplizerClass = localBindingContext().getBuildingOptions().getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( attributeSource.getExplicitTuplizerClassName() );
			attributeBinding.setCustomComponentTuplizerClass( tuplizerClass );
		}
		return attributeBinding;
	}

	/**
	 * todo: if the not found exception is ignored, here we should create an unique key instead of FK
	 * this guard method should be removed after we implement this.
	 */
	private void throwExceptionIfNotFoundIgnored(boolean isNotFoundAnException) {
		if ( !isNotFoundAnException ) {
			throw new NotYetImplementedException( "association of ignored not found exception is not yet implemented" );
		}
	}

	private ManyToOneAttributeBinding bindManyToOneAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ToOneAttributeSource attributeSource,
			SingularAttribute attribute) {
		if ( attribute == null ) {
			attribute = createSingularAttribute( attributeBindingContainer, attributeSource );
		}
		throwExceptionIfNotFoundIgnored( attributeSource.isNotFoundAnException() );

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
				attributeSource.isNotFoundAnException(),
				attributeSource.getNaturalIdMutability(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
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

		if ( referencedAttributeBinding != referencedEntityBinding.getHierarchyDetails()
				.getEntityIdentifier()
				.getAttributeBinding() ) {
			referencedAttributeBinding.setAlternateUniqueKey( true );
		}

		final SingularAttributeBinding idAttributeBinding =
				referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		final String uniqueKeyAttributeName = idAttributeBinding == referencedAttributeBinding ?
				null :
				getRelativePathFromEntityName( referencedAttributeBinding );

		final Type resolvedType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().manyToOne(
				referencedEntityBinding.getEntityName(),
				uniqueKeyAttributeName == null,
				uniqueKeyAttributeName,
				attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
				attributeSource.isUnWrapProxy(),
				!attributeSource.isNotFoundAnException(),
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

		final String uniqueKeyAttributeName;
		final AttributeBinding referencedEntityIdAttributeBinding =
				referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		if ( referencedEntityIdAttributeBinding == referencedAttributeBinding ||
				referencedAttributeBinding.getRelationalValueBindings().isEmpty()) {
			uniqueKeyAttributeName = null;
		}
		else {
			uniqueKeyAttributeName = getRelativePathFromEntityName( referencedAttributeBinding );
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
		final SingularAttributeBinding idAttributeBinding =
				attributeBinding.getReferencedEntityBinding().getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		if ( attributeBinding.getReferencedAttributeBinding() != idAttributeBinding ) {
				attributeBinding.getReferencedAttributeBinding().setAlternateUniqueKey( true );
		}
		attributeBinding.setCascadeStyle( determineCascadeStyle( attributeSource.getCascadeStyles() ) );
		attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
		attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ plural attributes binding
	private AbstractPluralAttributeBinding bindPluralAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource) {
		final PluralAttributeSource.Nature nature = attributeSource.getNature();
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
					localBindingContext().<CollectionPersister>locateClassByName(
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
				createMetaAttributeContext( attributeBindingContainer, attributeSource )
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
				getSequentialPluralAttributeIndexBase( attributeSource )
		);
	}

	private int getSequentialPluralAttributeIndexBase(IndexedPluralAttributeSource pluralAttributeSource) {
		final PluralAttributeIndexSource indexedPluralAttributeSource =  pluralAttributeSource.getIndexSource();
		if ( ! SequentialPluralAttributeIndexSource.class.isInstance( indexedPluralAttributeSource ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Expected an argument of type: %s; instead, got %s",
							SequentialPluralAttributeIndexSource.class.getName(),
							indexedPluralAttributeSource.getClass().getName()
					)
			);
		}
		return ( (SequentialPluralAttributeIndexSource) indexedPluralAttributeSource ).base();
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

				createMetaAttributeContext( attributeBindingContainer, attributeSource )
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
				createMetaAttributeContext( attributeBindingContainer, attributeSource )
		);
	}


	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ collection attributes binding

	private void bindBasicCollectionElement(
			final BasicPluralAttributeElementBinding elementBinding,
			final BasicPluralAttributeElementSource elementSource,
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
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.BASIC &&
				attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.AGGREGATE ) {
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

	private void bindCompositeCollectionElement(
			final CompositePluralAttributeElementBinding elementBinding,
			final CompositePluralAttributeElementSource elementSource,
			final JavaTypeDescriptor reflectedElementTypeDescriptor) {
		final PluralAttributeBinding pluralAttributeBinding = elementBinding.getPluralAttributeBinding();
		// Create the aggregate type
		// TODO: aggregateName should be set to elementSource.getPath() (which is currently not implemented)
		//       or Binder should define AttributeBindingContainer paths instead.
		final JavaTypeDescriptor elementTypeDescriptor;
		if ( elementSource.getTypeDescriptor() != null ) {
			elementTypeDescriptor = elementSource.getTypeDescriptor();
		}
		else {
			elementTypeDescriptor = reflectedElementTypeDescriptor;
		}
		final Aggregate aggregate = new Aggregate(
				elementTypeDescriptor,
				null
		);
		final SingularAttribute parentAttribute =
				StringHelper.isEmpty( elementSource.getParentReferenceAttributeName() )
						? null
						: aggregate.createSingularAttribute( elementSource.getParentReferenceAttributeName() );
		final CompositeAttributeBindingContainer compositeAttributeBindingContainer =
				elementBinding.createCompositeAttributeBindingContainer(
						aggregate,
						createMetaAttributeContext(
								pluralAttributeBinding.getContainer(),
								elementSource.getToolingHintSources()
						),
						parentAttribute
				);

		bindAttributes( compositeAttributeBindingContainer, elementSource );
		pluralAttributeBinding.getAttribute().setElementType( aggregate );
		Type resolvedType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel( rootBindingContext.getServiceRegistry(), compositeAttributeBindingContainer, false, false )
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
		for ( AttributeBinding ab : compositeAttributeBindingContainer.attributeBindings() ) {
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

	private void bindEntityAttributePluralAttributeIndex(
			final IndexedPluralAttributeBinding attributeBinding,
			final EntityAttributePluralAttributeIndexSource indexSource,
			final JavaTypeDescriptor defaultIndexTypeDescriptor) {
		throw new NotYetImplementedException( "Plural attribute index that is an attribute of the referenced entity is not supported yet." );
	}

	private void bindBasicCollectionIndex(
			final IndexedPluralAttributeBinding attributeBinding,
			final BasicPluralAttributeIndexSource indexSource,
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
								.getNature() != PluralAttributeElementBinding.Nature.ONE_TO_MANY
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
		final CompositePluralAttributeIndexSource indexSource =
				(CompositePluralAttributeIndexSource) indexedPluralAttributeSource.getIndexSource();
		final JavaTypeDescriptor indexTypeDescriptor;
		if ( indexSource.getTypeDescriptor() != null ) {
			indexTypeDescriptor = indexSource.getTypeDescriptor();
		}
		else {
			indexTypeDescriptor = reflectedIndexTypeDescriptor;
		}
		// Create the aggregate type
		// TODO: aggregateName should be set to elementSource.getPath() (which is currently not implemented)
		//       or Binder should define AttributeBindingContainer paths instead.
		final Aggregate aggregate = new Aggregate(
				indexTypeDescriptor,
				null
		);
		final CompositeAttributeBindingContainer compositeAttributeBindingContainer =
				indexBinding.createCompositeAttributeBindingContainer(
						aggregate,
						null,
						null
				);

		bindAttributes( compositeAttributeBindingContainer, indexSource );

		Type resolvedType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel(
						rootBindingContext.getServiceRegistry(),
						compositeAttributeBindingContainer,
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
			final OneToManyPluralAttributeElementBinding elementBinding,
			final OneToManyPluralAttributeElementSource elementSource,
			final EntityBinding referencedEntityBinding,
			final JavaTypeDescriptor defaultElementTypeDescriptor) {
		throwExceptionIfNotFoundIgnored( elementSource.isNotFoundAnException() );
		elementBinding.setElementEntityIdentifier(
				referencedEntityBinding.getKeyRelationalValueBindings()
		);

		Type resolvedElementType = rootBindingContext.getMetadataCollector().getTypeResolver().getTypeFactory().manyToOne(
				referencedEntityBinding.getEntityName(),
				true,
				null,
				false,
				false,
				!elementSource.isNotFoundAnException(), //TODO: should be attributeBinding.isIgnoreNotFound(),
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
			final ManyToManyPluralAttributeElementBinding elementBinding,
			final ManyToManyPluralAttributeElementSource elementSource,
			final EntityBinding referencedEntityBinding,
			final JavaTypeDescriptor defaultElementTypeDescriptor) {
		throwExceptionIfNotFoundIgnored( elementSource.isNotFoundAnException() );
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
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.ONE_TO_MANY ) {
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
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.MANY_TO_MANY ) {
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
				createAttributePath( attributeBinding ),
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
		if ( indexSource.isReferencedEntityAttribute() ) {
			bindEntityAttributePluralAttributeIndex(
					attributeBinding,
					(EntityAttributePluralAttributeIndexSource) indexSource,
					reflectedIndexTypeDescriptor
			);
		}
		else {
			switch ( attributeSource.getIndexSource().getNature() ) {
				case BASIC: {
					bindBasicCollectionIndex(
							attributeBinding,
							(BasicPluralAttributeIndexSource) attributeSource.getIndexSource(),
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
					.getNature() == PluralAttributeElementBinding.Nature.ONE_TO_MANY ) {
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
				(CompositePluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
				(CompositePluralAttributeElementSource) attributeSource.getElementSource(),
				typeHelper().reflectedCollectionElementJavaType( reflectedCollectionJavaTypes )
		);
	}

	private void bindManyToManyAttribute(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final ManyToManyPluralAttributeElementSource elementSource =
				(ManyToManyPluralAttributeElementSource) attributeSource.getElementSource();
		final JavaTypeDescriptor reflectedElementTypeDescriptor = typeHelper().reflectedCollectionElementJavaType(
				reflectedCollectionJavaTypes
		);
		if ( elementSource.getReferencedEntityName() == null && reflectedElementTypeDescriptor == null ) {
			throw localBindingContext().makeMappingException(
					String.format(
							"The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
							createAttributePathQualifiedByEntityName( attributeBinding )
					)
			);
		}
		EntityBinding referencedEntityBinding = locateEntityBinding(
				reflectedElementTypeDescriptor,
				elementSource.getReferencedEntityName()
		);
		ManyToManyPluralAttributeElementBinding manyToManyPluralAttributeElementBinding = (ManyToManyPluralAttributeElementBinding) attributeBinding
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
				(ManyToManyPluralAttributeElementSource) attributeSource.getElementSource(),
				referencedEntityBinding,
				reflectedElementTypeDescriptor
		);
	}

	private void bindOneToManyAttribute(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final OneToManyPluralAttributeElementSource elementSource =
				(OneToManyPluralAttributeElementSource) attributeSource.getElementSource();
		final JavaTypeDescriptor defaultElementTypeDescriptor = typeHelper().reflectedCollectionElementJavaType(
				reflectedCollectionJavaTypes
		);
		if ( elementSource.getReferencedEntityName() == null && defaultElementTypeDescriptor == null ) {
			throw localBindingContext().makeMappingException(
					String.format(
							"The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
							createAttributePathQualifiedByEntityName( attributeBinding )
					)
			);
		}
		EntityBinding referencedEntityBinding = locateEntityBinding(
				defaultElementTypeDescriptor,
				elementSource.getReferencedEntityName()
		);
		bindOneToManyCollectionKey( attributeBinding, attributeSource, referencedEntityBinding );
		bindOneToManyCollectionElement(
				(OneToManyPluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
				(OneToManyPluralAttributeElementSource) attributeSource.getElementSource(),
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
				(BasicPluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
				(BasicPluralAttributeElementSource) attributeSource.getElementSource(),
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
				Class<Comparator<?>> comparatorClass =
						localBindingContext().locateClassByName( sortable.getComparatorName() );
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
			final BasicPluralAttributeElementBinding elementBinding) {
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
		if ( !attributeBinding.getPluralAttributeElementBinding().hasNonNullableValue() ) {
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
		for ( final RelationalValueBinding elementValueBinding : elementBinding.getRelationalValueBindings() ) {
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
		final PluralAttributeSource.Nature pluralAttributeSourceNature = attributeSource.getNature();
		final PluralAttributeElementSource.Nature pluralElementSourceNature = attributeSource.getElementSource().getNature();

		//TODO what is this case? it would be really good to add a comment
		if ( pluralElementSourceNature == PluralAttributeElementSource.Nature.ONE_TO_MANY
				|| pluralAttributeSourceNature == PluralAttributeSource.Nature.BAG ) {
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


	private String propertyAccessorName(final AttributeSource attributeSource) {
		return propertyAccessorName( attributeSource.getPropertyAccessorName() );
	}

	private String propertyAccessorName(final String propertyAccessorName) {
		return propertyAccessorName == null
				? localBindingContext().getMappingDefaults().getPropertyAccessorName()
				: propertyAccessorName;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ static methods
	private static TableSpecification locateDefaultTableSpecificationForAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource) {
		return attributeSource.getContainingTableName() == null ?
				attributeBindingContainer.getPrimaryTable() :
				attributeBindingContainer.seekEntityBinding().locateTable( attributeSource.getContainingTableName() );
	}

	private static PluralAttributeElementBinding.Nature pluralAttributeElementNature(
			final PluralAttributeSource attributeSource) {
		return PluralAttributeElementBinding.Nature.valueOf( attributeSource.getElementSource().getNature().name() );
	}

	private static PluralAttributeIndexBinding.Nature pluralAttributeIndexNature(
			final PluralAttributeSource attributeSource) {
		if ( !IndexedPluralAttributeSource.class.isInstance( attributeSource ) ) {
			return null;
		}
		return PluralAttributeIndexBinding.Nature.valueOf(
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
			if ( pluralAttributeElementBinding.hasNonNullableValue() ) {
				for ( final RelationalValueBinding keyRelationalValueBinding : keyRelationalValueBindings ) {
					primaryKey.addColumn( (Column) keyRelationalValueBinding.getValue() );
				}
				for ( RelationalValueBinding relationalValueBinding : pluralAttributeElementBinding.getRelationalValueBindings() ) {
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


	private static String getRelativePathFromEntityName(
			final AttributeBinding attributeBinding) {
		return StringHelper.isEmpty(  attributeBinding.getContainer().getPathBase() ) ?
				attributeBinding.getAttribute().getName() :
				attributeBinding.getContainer().getPathBase() + "." + attributeBinding.getAttribute().getName();
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

	private static MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSource attributeSource) {
		return createMetaAttributeContext( attributeBindingContainer, attributeSource.getToolingHintSources() );
	}

	private static MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final Iterable<? extends ToolingHintSource> metaAttributeSources) {
		return createMetaAttributeContext(
				metaAttributeSources,
				false,
				attributeBindingContainer.getMetaAttributeContext()
		);
	}

	private static MetaAttributeContext createMetaAttributeContext(
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

	private static SingularAttribute createSingularAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource) {
		return attributeSource.isVirtualAttribute() ?
				attributeBindingContainer.getAttributeContainer().createSyntheticSingularAttribute(
						attributeSource.getName()
				) :
				attributeBindingContainer.getAttributeContainer().createSingularAttribute( attributeSource.getName() );
	}

	static String createAttributePathQualifiedByEntityName(final AttributeBinding attributeBinding) {
		final String entityName = attributeBinding.getContainer().seekEntityBinding().getEntityName();
		return entityName + '.' + createAttributePath( attributeBinding );
	}
	static String createAttributePath(final AttributeBinding attributeBinding) {
		return StringHelper.isEmpty( attributeBinding.getContainer().getPathBase() ) ?
				attributeBinding.getAttribute().getName() :
				attributeBinding.getContainer().getPathBase() + '.' + attributeBinding.getAttribute().getName();
	}

	public static interface DefaultNamingStrategy {

		String defaultName(NamingStrategy namingStrategy);
	}
}
