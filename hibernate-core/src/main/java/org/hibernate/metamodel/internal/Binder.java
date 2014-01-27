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
package org.hibernate.metamodel.internal;

import static org.hibernate.engine.spi.SyntheticAttributeHelper.SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME;

import java.util.ArrayList;
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
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
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
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.EntityHierarchyHelper.LocalBindingContextExecutionContext;
import org.hibernate.metamodel.internal.EntityHierarchyHelper.LocalBindingContextExecutor;
import org.hibernate.metamodel.internal.HibernateTypeHelper.ReflectedCollectionJavaTypes;
import org.hibernate.metamodel.internal.resolver.AssociationRelationalBindingResolver;
import org.hibernate.metamodel.internal.resolver.MappedByAssociationRelationalBindingResolverImpl;
import org.hibernate.metamodel.internal.resolver.StandardAssociationRelationalBindingResolverImpl;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.Cascadeable;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.CompositePluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.CompositePluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.EntityVersion;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
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
import org.hibernate.metamodel.spi.binding.SingularNonAssociationAttributeBinding;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.IndexedPluralAttribute;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.CompositePluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.CompositePluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.DerivedValueSource;
import org.hibernate.metamodel.spi.source.DiscriminatorSource;
import org.hibernate.metamodel.spi.source.EntityAttributePluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.FilterSource;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.IndexConstraintSource;
import org.hibernate.metamodel.spi.source.IndexedPluralAttributeSource;
import org.hibernate.metamodel.spi.source.JoinedSubclassEntitySource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.MappedByAssociationSource;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.MultiTenancySource;
import org.hibernate.metamodel.spi.source.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.OneToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.Orderable;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.RelationalValueSourceContainer;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SequentialPluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.SimpleIdentifierSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.Sortable;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;
import org.hibernate.metamodel.spi.source.UniqueConstraintSource;
import org.hibernate.metamodel.spi.source.VersionAttributeSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.tuple.component.ComponentTuplizer;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * The common binder shared between annotations and {@code hbm.xml} processing.
 * <p/>
 * The API consists of {@link #Binder(org.hibernate.metamodel.spi.MetadataImplementor, IdentifierGeneratorFactory)} and {@link #bindEntityHierarchies}
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 * @author Brett Meyer
 * @author Strong Liu
 */
public class Binder implements HelperContext {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			Binder.class.getName()
	);

	private final IdentifierGeneratorFactory identifierGeneratorFactory;

	// Entity hierarchies and source index need be available throughout the binding process
	private final Map<String, EntityHierarchy> entityHierarchiesByRootEntityName =
			new LinkedHashMap<String, EntityHierarchy>();
	private final SourceIndex sourceIndex = new SourceIndex();

	// todo : apply org.hibernate.metamodel.MetadataSources.getExternalCacheRegionDefinitions()

	private final MetadataImplementor metadata;

	private final EntityHierarchyHelper.LocalBindingContextManager localBindingContextManager;

	private final EntityHierarchyHelper entityHierarchyHelper;
	private final HibernateTypeHelper typeHelper; // todo: refactor helper and remove redundant methods in this class
	private final RelationalIdentifierHelper relationalIdentifierHelper;
	private final TableHelper tableHelper;
	private final ForeignKeyHelper foreignKeyHelper;
	private final RelationalValueBindingHelper relationalValueBindingHelper;
	private final NaturalIdUniqueKeyHelper naturalIdUniqueKeyHelper;

	private final StandardAssociationRelationalBindingResolverImpl standardAssociationRelationalBindingResolver;
	private final MappedByAssociationRelationalBindingResolverImpl mappedByAssociationRelationalBindingResolver;

	public Binder(final MetadataImplementor metadata,
				  final IdentifierGeneratorFactory identifierGeneratorFactory) {
		this.metadata = metadata;
		this.identifierGeneratorFactory = identifierGeneratorFactory;
		this.localBindingContextManager = new LocalBindingContextManagerImpl();
		this.entityHierarchyHelper = new EntityHierarchyHelper( localBindingContextManager );
		this.typeHelper = new HibernateTypeHelper( this );
		this.relationalIdentifierHelper = new RelationalIdentifierHelper( this );
		this.tableHelper = new TableHelper( this );
		this.foreignKeyHelper = new ForeignKeyHelper( this );
		this.relationalValueBindingHelper = new RelationalValueBindingHelper( this );
		this.naturalIdUniqueKeyHelper = new NaturalIdUniqueKeyHelper( this );
		this.standardAssociationRelationalBindingResolver =
				new StandardAssociationRelationalBindingResolverImpl( this );
		this.mappedByAssociationRelationalBindingResolver =
				new MappedByAssociationRelationalBindingResolverImpl( this );

	}

	/**
	 * The entry point of {@linkplain Binder} class, adds all the entity hierarchy one by one.
	 *
	 * Indexes all {@link EntitySource} objects in an {@link EntityHierarchy} and
	 * creates all {@link EntityBinding}.
	 *
	 * @param entityHierarchies The entity hierarchies resolved from mappings
	 */
	public void addEntityHierarchies(final Iterable<EntityHierarchy> entityHierarchies) {

		LocalBindingContextExecutor executor = new LocalBindingContextExecutor() {
			@Override
			public void execute(LocalBindingContextExecutionContext bindingContextContext) {
				sourceIndex.indexEntitySource(
						bindingContextContext.getRootEntitySource(),
						bindingContextContext.getEntitySource()
				);
				createEntityBinding(
						bindingContextContext.getSuperEntityBinding(),
						bindingContextContext.getEntitySource(),
						bindingContextContext.getInheritanceType(),
						bindingContextContext.getEntityMode()
				);
			}
			private void resolveEntityLaziness(
					final EntityBinding entityBinding,
					final EntitySource entitySource,
					final EntityMode entityMode) {
				if ( entityMode == EntityMode.POJO ) {
					final String proxy = entitySource.getProxy();
					if ( proxy == null ) {
						if ( entitySource.isLazy() ) {
							entityBinding.setProxyInterfaceType( entityBinding.getEntity().getClassReferenceUnresolved() );
							entityBinding.setLazy( true );
						}
					}
					else {
						entityBinding.setProxyInterfaceType(
								bindingContext().makeClassReference(
										bindingContext().qualifyClassName(
												proxy
										)
								)
						);
						entityBinding.setLazy( true );
					}
				}
				else {
					entityBinding.setProxyInterfaceType( null );
					entityBinding.setLazy( entitySource.isLazy() );
				}
			}
			private EntityBinding createEntityBinding(
					final EntityBinding superEntityBinding,
					final EntitySource entitySource,
					final InheritanceType inheritanceType,
					final EntityMode entityMode) {
				// Create binding
				final EntityBinding entityBinding =
						entitySource instanceof RootEntitySource ?
								new EntityBinding( inheritanceType, entityMode ) :
								new EntityBinding( superEntityBinding );
				// Create domain entity
				final String entityClassName = entityMode == EntityMode.POJO ? entitySource.getClassName() : null;
				LocalBindingContext bindingContext = bindingContext();
				entityBinding.setEntity(
						new Entity(
								entitySource.getEntityName(),
								entityClassName,
								bindingContext.makeClassReference( entityClassName ),
								superEntityBinding == null ? null : superEntityBinding.getEntity()
						)
				);

				entityBinding.setEntityName( entitySource.getEntityName() );
				entityBinding.setJpaEntityName( entitySource.getJpaEntityName() );          //must before creating primary table
				entityBinding.setDynamicUpdate( entitySource.isDynamicUpdate() );
				entityBinding.setDynamicInsert( entitySource.isDynamicInsert() );
				entityBinding.setBatchSize( entitySource.getBatchSize() );
				entityBinding.setSelectBeforeUpdate( entitySource.isSelectBeforeUpdate() );
				entityBinding.setAbstract( entitySource.isAbstract() );
				entityBinding.setCustomLoaderName( entitySource.getCustomLoaderName() );
				entityBinding.setCustomInsert( entitySource.getCustomSqlInsert() );
				entityBinding.setCustomUpdate( entitySource.getCustomSqlUpdate() );
				entityBinding.setCustomDelete( entitySource.getCustomSqlDelete() );
				entityBinding.setJpaCallbackClasses( entitySource.getJpaCallbackClasses() );

				// todo: deal with joined and unioned subclass bindings
				// todo: bind fetch profiles
				// Configure rest of binding
				final String customTuplizerClassName = entitySource.getCustomTuplizerClassName();
				if ( customTuplizerClassName != null ) {
					entityBinding.setCustomEntityTuplizerClass(
							bindingContext.<EntityTuplizer>locateClassByName(
									customTuplizerClassName
							)
					);
				}
				final String customPersisterClassName = entitySource.getCustomPersisterClassName();
				if ( customPersisterClassName != null ) {
					entityBinding.setCustomEntityPersisterClass(
							bindingContext.<EntityPersister>locateClassByName(
									customPersisterClassName
							)
					);
				}
				entityBinding.setMetaAttributeContext(
						createMetaAttributeContext(
								entitySource.getMetaAttributeSources(),
								true,
								metadata.getGlobalMetaAttributeContext()
						)
				);

				if ( entitySource.getSynchronizedTableNames() != null ) {
					entityBinding.addSynchronizedTableNames( entitySource.getSynchronizedTableNames() );
				}
				resolveEntityLaziness( entityBinding, entitySource, entityMode );
				if ( entitySource.getFilterSources() != null ) {
					for ( FilterSource filterSource : entitySource.getFilterSources() ) {
						entityBinding.addFilterConfiguration( createFilterConfiguration( filterSource, entityBinding ) );
					}
				}
				// Register binding with metadata
				metadata.addEntity( entityBinding );
				return entityBinding;
			}
		};
		for ( EntityHierarchy entityHierarchy : entityHierarchies ) {
			entityHierarchiesByRootEntityName.put(
					entityHierarchy.getRootEntitySource().getEntityName(),
					entityHierarchy
			);
			entityHierarchyHelper.applyToEntityHierarchy( entityHierarchy, executor, executor );
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

	@Override
	public LocalBindingContext bindingContext() {
		return localBindingContextManager.localBindingContext();
	}

	@Override
	public HibernateTypeHelper typeHelper() {
		return typeHelper;
	}

	@Override
	public RelationalIdentifierHelper relationalIdentifierHelper() {
		return relationalIdentifierHelper;
	}

	@Override
	public TableHelper tableHelper() {
		return tableHelper;
	}

	@Override
	public ForeignKeyHelper foreignKeyHelper() {
		return foreignKeyHelper;
	}

	@Override
	public RelationalValueBindingHelper relationalValueBindingHelper() {
		return relationalValueBindingHelper;
	}

	@Override
	public NaturalIdUniqueKeyHelper naturalIdUniqueKeyHelper() {
		return naturalIdUniqueKeyHelper;
	}

	private LocalBindingContextExecutor bindIdentifierGeneratorExecutor() {
		return new LocalBindingContextExecutor() {
			@Override
			public void execute(LocalBindingContextExecutionContext bindingContextContext) {
				EntityBinding entityBinding = bindingContextContext.getEntityBinding();
				if ( entityBinding.getSuperEntityBinding() == null ) {
					bindIdentifierGenerator( entityBinding );
				}
			}
		};
	}

	private void applyToAllEntityHierarchies(LocalBindingContextExecutor executor) {
		applyToAllEntityHierarchies( executor, executor );
	}

	private void applyToAllEntityHierarchies(LocalBindingContextExecutor rootExecutor, LocalBindingContextExecutor subExecutor) {
		entityHierarchyHelper.applyToAllEntityHierarchies(
				entityHierarchiesByRootEntityName.values(),
				rootExecutor,
				subExecutor
		);
	}


	private void bindEntityHierarchiesExcludingNonIdAttributeBindings() {
		LocalBindingContextExecutor rootEntityCallback = new LocalBindingContextExecutor() {
			@Override
			public void execute(LocalBindingContextExecutionContext bindingContextContext) {
				final RootEntitySource rootEntitySource = (RootEntitySource) bindingContextContext.getEntitySource();
				final EntityBinding rootEntityBinding = bindingContextContext.getEntityBinding();
				bindPrimaryTable( rootEntityBinding, rootEntitySource );
				// Create/Bind root-specific information
				bindIdentifier( rootEntityBinding, rootEntitySource.getIdentifierSource() );
				bindSecondaryTables( rootEntityBinding, rootEntitySource );
				bindVersion( rootEntityBinding, rootEntitySource.getVersioningAttributeSource() );
				bindDiscriminator( rootEntityBinding, rootEntitySource );
				bindMultiTenancy( rootEntityBinding, rootEntitySource );
				rootEntityBinding.getHierarchyDetails().setCaching( rootEntitySource.getCaching() );
				rootEntityBinding.getHierarchyDetails().setNaturalIdCaching( rootEntitySource.getNaturalIdCaching() );
				rootEntityBinding.getHierarchyDetails()
						.setExplicitPolymorphism( rootEntitySource.isExplicitPolymorphism() );
				rootEntityBinding.getHierarchyDetails().setOptimisticLockStyle( rootEntitySource.getOptimisticLockStyle() );
				rootEntityBinding.setMutable( rootEntitySource.isMutable() );
				rootEntityBinding.setWhereFilter( rootEntitySource.getWhere() );
				rootEntityBinding.setRowId( rootEntitySource.getRowId() );
			}

			private void bindSimpleIdentifier(
					final EntityBinding rootEntityBinding,
					final SimpleIdentifierSource identifierSource) {
				// locate the attribute binding
				final BasicAttributeBinding idAttributeBinding = (BasicAttributeBinding) bindIdentifierAttribute(
						rootEntityBinding, identifierSource.getIdentifierAttributeSource()
				);

				// Configure ID generator
				IdentifierGeneratorDefinition generator = identifierSource.getIdentifierGeneratorDescriptor();
				if ( generator == null ) {
					final Map<String, String> params = new HashMap<String, String>();
					params.put( IdentifierGenerator.ENTITY_NAME, rootEntityBinding.getEntity().getName() );
					generator = new IdentifierGeneratorDefinition( "default_assign_identity_generator", "assigned", params );
				}

				// determine the unsaved value mapping
				final String unsavedValue = interpretIdentifierUnsavedValue( identifierSource, generator );

				rootEntityBinding.getHierarchyDetails().getEntityIdentifier().prepareAsSimpleIdentifier(
						idAttributeBinding,
						generator,
						unsavedValue
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
					final EntityBinding rootEntityBinding,
					final AggregatedCompositeIdentifierSource identifierSource) {
				// locate the attribute binding
				final CompositeAttributeBinding idAttributeBinding =
						(CompositeAttributeBinding) bindIdentifierAttribute(
								rootEntityBinding, identifierSource.getIdentifierAttributeSource()
						);

				// Configure ID generator
				IdentifierGeneratorDefinition generator = identifierSource.getIdentifierGeneratorDescriptor();
				if ( generator == null ) {
					final Map<String, String> params = new HashMap<String, String>();
					params.put( IdentifierGenerator.ENTITY_NAME, rootEntityBinding.getEntity().getName() );
					generator = new IdentifierGeneratorDefinition( "default_assign_identity_generator", "assigned", params );
				}

				// determine the unsaved value mapping
				final String unsavedValue = interpretIdentifierUnsavedValue( identifierSource, generator );

				rootEntityBinding.getHierarchyDetails().getEntityIdentifier().prepareAsAggregatedCompositeIdentifier(
						idAttributeBinding, generator, unsavedValue
				);
			}

			private void bindNonAggregatedCompositeIdentifier(
					final EntityBinding rootEntityBinding,
					final NonAggregatedCompositeIdentifierSource identifierSource) {
				// locate the attribute bindings for the real attributes
				List<SingularAttributeBinding> idAttributeBindings =
						new ArrayList<SingularAttributeBinding>();
				for ( SingularAttributeSource attributeSource : identifierSource.getAttributeSourcesMakingUpIdentifier() ) {
					SingularAttributeBinding singularAttributeBinding =
							bindIdentifierAttribute( rootEntityBinding, attributeSource );
					idAttributeBindings.add( singularAttributeBinding );
				}

				final Class<?> idClassClass = identifierSource.getLookupIdClass();
				final String idClassPropertyAccessorName =
						idClassClass == null ?
								null :
								propertyAccessorName( identifierSource.getIdClassPropertyAccessorName() );

				// Configure ID generator
				IdentifierGeneratorDefinition generator = identifierSource.getIdentifierGeneratorDescriptor();
				if ( generator == null ) {
					final Map<String, String> params = new HashMap<String, String>();
					params.put( IdentifierGenerator.ENTITY_NAME, rootEntityBinding.getEntity().getName() );
					generator = new IdentifierGeneratorDefinition( "default_assign_identity_generator", "assigned", params );
				}
				// Create the synthetic attribute
				final SingularAttribute syntheticAttribute =
						rootEntityBinding.getEntity().createSyntheticCompositeAttribute(
								SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME,
								rootEntityBinding.getEntity()
						);

				final CompositeAttributeBinding syntheticAttributeBinding =
						rootEntityBinding.makeVirtualCompositeAttributeBinding(
								syntheticAttribute,
								createMetaAttributeContext( rootEntityBinding, identifierSource.getMetaAttributeSources() ),
								idAttributeBindings
						);
				// Create the synthetic attribute binding.
				rootEntityBinding.getHierarchyDetails().getEntityIdentifier().prepareAsNonAggregatedCompositeIdentifier(
						syntheticAttributeBinding,
						generator,
						interpretIdentifierUnsavedValue( identifierSource, generator ),
						idClassClass,
						idClassPropertyAccessorName
				);

				typeHelper.bindNonAggregatedCompositeIdentifierType( syntheticAttributeBinding, syntheticAttribute );
			}

			private void bindIdentifier(
					final EntityBinding rootEntityBinding,
					final IdentifierSource identifierSource) {
				final EntityIdentifierNature nature = identifierSource.getNature();
				switch ( nature ) {
					case SIMPLE: {
						bindSimpleIdentifier( rootEntityBinding, (SimpleIdentifierSource) identifierSource );
						break;
					}
					case AGGREGATED_COMPOSITE: {
						bindAggregatedCompositeIdentifier(
								rootEntityBinding,
								(AggregatedCompositeIdentifierSource) identifierSource
						);
						break;
					}
					case NON_AGGREGATED_COMPOSITE: {
						bindNonAggregatedCompositeIdentifier(
								rootEntityBinding,
								(NonAggregatedCompositeIdentifierSource) identifierSource
						);
						break;
					}
					default: {
						throw bindingContext().makeMappingException( "Unknown identifier nature : " + nature.name() );
					}
				}
			}

			private void bindVersion(
					final EntityBinding rootEntityBinding,
					final VersionAttributeSource versionAttributeSource) {
				if ( versionAttributeSource == null ) {
					return;
				}
				final EntityVersion version = rootEntityBinding.getHierarchyDetails().getEntityVersion();
				version.setVersioningAttributeBinding(
						(BasicAttributeBinding) bindAttribute(
								rootEntityBinding,
								versionAttributeSource
						)
				);
				// ensure version is non-nullable
				for ( RelationalValueBinding valueBinding : version.getVersioningAttributeBinding()
						.getRelationalValueBindings() ) {
					if ( !valueBinding.isDerived() ) {
						( (Column) valueBinding.getValue() ).setNullable( false );
					}
				}
				version.setUnsavedValue(
						versionAttributeSource.getUnsavedValue() == null ? "undefined" : versionAttributeSource.getUnsavedValue()
				);
			}
			private void bindMultiTenancy(
					final EntityBinding rootEntityBinding,
					final RootEntitySource rootEntitySource) {
				final MultiTenancySource multiTenancySource = rootEntitySource.getMultiTenancySource();
				if ( multiTenancySource == null ) {
					return;
				}

				// if (1) the strategy is discriminator based and (2) the entity is not shared, we need to either (a) extract
				// the user supplied tenant discriminator value mapping or (b) generate an implicit one
				final boolean needsTenantIdentifierValueMapping =
						MultiTenancyStrategy.DISCRIMINATOR == metadata.getOptions().getMultiTenancyStrategy()
								&& !multiTenancySource.isShared();

				if ( needsTenantIdentifierValueMapping ) {
					// NOTE : the table for tenant identifier/discriminator is always the primary table
					final Value tenantDiscriminatorValue;
					final RelationalValueSource valueSource = multiTenancySource.getRelationalValueSource();
					if ( valueSource == null ) {
						// user supplied no explicit information, so use implicit mapping with default name
						tenantDiscriminatorValue = rootEntityBinding.getPrimaryTable().locateOrCreateColumn(
								metadata.getMappingDefaults().getTenantIdColumnName()
						);
					}
					else {
						tenantDiscriminatorValue = buildDiscriminatorRelationValue(
								valueSource,
								rootEntityBinding.getPrimaryTable()
						);
					}
					rootEntityBinding.getHierarchyDetails()
							.getTenantDiscrimination()
							.setDiscriminatorValue( tenantDiscriminatorValue );
				}

				rootEntityBinding.getHierarchyDetails().getTenantDiscrimination().setShared( multiTenancySource.isShared() );
				rootEntityBinding.getHierarchyDetails()
						.getTenantDiscrimination()
						.setUseParameterBinding( multiTenancySource.bindAsParameter() );
			}
			private Value buildDiscriminatorRelationValue(
					final RelationalValueSource valueSource,
					final TableSpecification table) {
				if ( valueSource.getNature() == RelationalValueSource.Nature.COLUMN ) {
					return tableHelper.locateOrCreateColumn(
							table,
							(ColumnSource) valueSource,
							new ColumnNamingStrategyHelper(
									bindingContext().getMappingDefaults().getDiscriminatorColumnName(),
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


			private void bindDiscriminator(
					final EntityBinding rootEntityBinding,
					final RootEntitySource rootEntitySource) {
				final DiscriminatorSource discriminatorSource = rootEntitySource.getDiscriminatorSource();
				if ( discriminatorSource == null ) {
					return;
				}
				final RelationalValueSource valueSource = discriminatorSource.getDiscriminatorRelationalValueSource();
				final TableSpecification table = rootEntityBinding.locateTable( valueSource.getContainingTableName() );
				final Value value = buildDiscriminatorRelationValue( valueSource, table );
				final EntityDiscriminator discriminator =
						new EntityDiscriminator( value, discriminatorSource.isInserted(), discriminatorSource.isForced() );
				rootEntityBinding.getHierarchyDetails().setEntityDiscriminator( discriminator );
				final String discriminatorValue = rootEntitySource.getDiscriminatorMatchValue();
				if ( discriminatorValue != null ) {
					rootEntityBinding.setDiscriminatorMatchValue( discriminatorValue );
				}
				else
				//		if ( !Modifier.isAbstract(
				//				bindingContext().locateClassByName( rootEntitySource.getEntityName() )
				//						.getModifiers()
				//		) )
				{
					// Use the class name as a default if no discriminator value.
					// However, skip abstract classes -- obviously no discriminators there.
					// But this is not correct, since for single table entity hierarchy, even the root class is abstract, it still should has discriminator
					rootEntityBinding.setDiscriminatorMatchValue( StringHelper.unqualify( rootEntitySource.getEntityName() ) );
				}
				// Configure discriminator hibernate type
				typeHelper.bindDiscriminatorType( discriminator, value );
			}

		};
		LocalBindingContextExecutor subEntityCallback = new LocalBindingContextExecutor() {
			@Override
			public void execute(LocalBindingContextExecutionContext bindingContextContext) {
				final EntitySource entitySource = bindingContextContext.getEntitySource();
				final EntityBinding entityBinding = bindingContextContext.getEntityBinding();
				final EntityBinding superEntityBinding = bindingContextContext.getSuperEntityBinding();
				entityBinding.setMutable( entityBinding.getHierarchyDetails().getRootEntityBinding().isMutable() );
				markSuperEntityTableAbstractIfNecessary( superEntityBinding );
				bindPrimaryTable( entityBinding, entitySource );
				bindSubEntityPrimaryKey( entityBinding, entitySource );
				bindSecondaryTables( entityBinding, entitySource );
			}
			private void bindSubEntityPrimaryKey(
					final EntityBinding entityBinding,
					final EntitySource entitySource) {
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
							foreignKeyHelper.determineForeignKeyTargetColumns(
									superEntityBinding,
									subclassEntitySource
							);

					ForeignKey foreignKey = locateOrCreateForeignKey(
							subclassEntitySource.getExplicitForeignKeyName(),
							table,
							joinRelationalValueBindings,
							foreignKeyHelper.determineForeignKeyTargetTable( superEntityBinding, subclassEntitySource ),
							targetColumns
					);

					if ( subclassEntitySource.isCascadeDeleteEnabled() ) {
						foreignKey.setDeleteRule( ForeignKey.ReferentialAction.CASCADE );
						entityBinding.setCascadeDeleteEnabled( true );
					}
				}
			}
			private  void markSuperEntityTableAbstractIfNecessary(
					final EntityBinding superEntityBinding) {
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
		// TODO: need to determine the proper order for processing EntityHierarchy objects
		//       so that dependent EntityHierarchy is processed after the EntityHierarchy it
		//       is dependent on.
		// For now, just delay processing the dependent entity hierarchies.
		Set<EntityHierarchy> unresolvedEntityHierarchies = new HashSet<EntityHierarchy>( );
		for ( final EntityHierarchy entityHierarchy : entityHierarchiesByRootEntityName.values() ) {
			if ( isIdentifierDependentOnOtherEntityHierarchy( entityHierarchy ) ) {
				unresolvedEntityHierarchies.add( entityHierarchy );
			}
			else {
				entityHierarchyHelper.applyToEntityHierarchy( entityHierarchy, rootEntityCallback, subEntityCallback );
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
			for ( Iterator<EntityHierarchy> it = unresolvedEntityHierarchies.iterator(); it.hasNext(); ) {
				final EntityHierarchy entityHierarchy = it.next();
				try {
					entityHierarchyHelper.applyToEntityHierarchy(
							entityHierarchy,
							rootEntityCallback,
							subEntityCallback
					);
					// succeeded, so the entityHierarchy is no longer unresolved.
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
	private boolean isIdentifierDependentOnOtherEntityHierarchy(EntityHierarchy entityHierarchy) {
		final RootEntitySource rootEntitySource = entityHierarchy.getRootEntitySource();
		final IdentifierSource identifierSource = rootEntitySource.getIdentifierSource();
		if ( identifierSource.getNature() != EntityIdentifierNature.SIMPLE ) {
			List<? extends AttributeSource> subAttributeSources =
					identifierSource.getNature() == EntityIdentifierNature.AGGREGATED_COMPOSITE ?
							( (AggregatedCompositeIdentifierSource) identifierSource ).getIdentifierAttributeSource().attributeSources() :
							( (NonAggregatedCompositeIdentifierSource) identifierSource ).getAttributeSourcesMakingUpIdentifier();
			return containsSingularAssociation( subAttributeSources );
		}
		else {
			return false;
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
		return StringHelper.isEmpty( containerPath ) ?
				entityBinding :
				(AttributeBindingContainer) entityBinding.locateAttributeBindingByPath( containerPath, false );
	}

	private LocalBindingContextExecutor resolveAssociationSourcesExecutor() {
		return new LocalBindingContextExecutor() {
			@Override
			public void execute(LocalBindingContextExecutionContext bindingContextContext) {
				sourceIndex.resolveAssociationSources( bindingContextContext );
			}
		};
	}

	private LocalBindingContextExecutor bindSingularAttributesExecutor(
			final boolean isMappedBy,
			final SingularAttributeSource.Nature nature) {
		return new LocalBindingContextExecutor() {
			@Override
			public void execute(LocalBindingContextExecutionContext bindingContextContext) {
				bindSingularAttributes(
						bindingContextContext.getEntityBinding(),
						isMappedBy,
						nature
				);
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
						null );
			}
			else if ( attributeBindingContainer instanceof CompositeAttributeBinding ) {
				// This attribute source is within a composite; skip binding if it is the parent.
				final CompositeAttributeBinding compositeAttributeBinding = (CompositeAttributeBinding) attributeBindingContainer;
				final ComponentAttributeSource compositeAttributeSource =
						(ComponentAttributeSource) sourceIndex.attributeSource(
								entityBinding.getEntityName(),
								compositeAttributeBinding.getPathBase()
						);
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
			ComponentAttributeSource compositeAttributeSource
	) {
		// Find out the number of sub-attributes, excluding the parent attribute.
		final int nAttributeSourcesExcludingParent =
				compositeAttributeBinding.getParentReference() != null ?
						compositeAttributeSource.attributeSources().size() - 1 :
						compositeAttributeSource.attributeSources().size();
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
				typeHelper.bindAggregatedCompositeAttributeType(
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

	private LocalBindingContextExecutor bindPluralAttributesExecutor(final boolean isInverse) {
		 return new LocalBindingContextExecutor() {
			@Override
			public void execute(LocalBindingContextExecutionContext bindingContextContext) {
				// TODO: may want bind plural attributes of a particular element nature.
				final EntityBinding entityBinding = bindingContextContext.getEntityBinding();
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


	private LocalBindingContextExecutor bindConstraintsExecutor() {
		return new LocalBindingContextExecutor() {
			@Override
			public void execute(LocalBindingContextExecutionContext bindingContextContext) {
				final EntityBinding entityBinding = bindingContextContext.getEntityBinding();
				final EntitySource entitySource = bindingContextContext.getEntitySource();
					for ( final ConstraintSource constraintSource : entitySource.getConstraints() ) {
						if ( UniqueConstraintSource.class.isInstance( constraintSource ) ) {
							final UniqueConstraintSource uniqueConstraintSource = (UniqueConstraintSource) constraintSource;

							final TableSpecification table = findConstraintTable( entityBinding, constraintSource.getTableName() );

							final List<Column> columns = new ArrayList<Column>();
							for ( final String columnName : uniqueConstraintSource.columnNames() ) {
								columns.add( tableHelper.locateOrCreateColumn( table, columnName,
										new ColumnNamingStrategyHelper( null, false ) ) );
							}
							tableHelper.createUniqueKey( table, columns, constraintSource.name() );
						}
						else if ( IndexConstraintSource.class.isInstance( constraintSource ) ) {
							final IndexConstraintSource indexConstraintSource = (IndexConstraintSource) constraintSource;

							final TableSpecification table = findConstraintTable( entityBinding, constraintSource.getTableName() );

							final List<Column> columns = new ArrayList<Column>();
							for ( final String columnName : indexConstraintSource.columnNames() ) {
								columns.add( tableHelper.locateOrCreateColumn( table, columnName,
										new ColumnNamingStrategyHelper( null, false ) ) );
							}
							tableHelper.createIndex( table, columns, constraintSource.name() );
						}
					}
				}
		};
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Entity binding relates methods

	private EntityBinding locateEntityBinding(
			final ValueHolder<Class<?>> entityJavaTypeValue,
			final String explicitEntityName) {
		final String referencedEntityName =
				explicitEntityName != null
						? explicitEntityName
						: entityJavaTypeValue.getValue().getName();
		return locateEntityBinding( referencedEntityName );
	}

	private EntityBinding locateEntityBinding(final String entityName) {
		// Check if binding has already been created
		EntityBinding entityBinding = metadata.getEntityBinding( entityName );
		if ( entityBinding == null ) {
			 throw bindingContext().makeMappingException(
					 String.format( "No entity binding with name: %s", entityName )
			 );
		}
		return entityBinding;
	}



	private FilterConfiguration createFilterConfiguration(FilterSource filterSource, EntityBinding entityBinding){
		String condition = filterSource.getCondition();
		if(StringHelper.isEmpty( condition )){
			FilterDefinition filterDefinition = metadata.getFilterDefinitions().get( filterSource.getName() );
			if(filterDefinition == null){
				throw bindingContext().makeMappingException( String.format( "Filter[%s] doesn't have a condition", filterSource.getName() ) );
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
		// single table and sub entity
		if ( superEntityBinding != null && inheritanceType == InheritanceType.SINGLE_TABLE ) {
			table = superEntityBinding.getPrimaryTable();
			tableName = superEntityBinding.getPrimaryTableName();
			// Configure discriminator if present
			final String discriminatorValue = entitySource.getDiscriminatorMatchValue() != null ?
					entitySource.getDiscriminatorMatchValue()
					: StringHelper.unqualify( entitySource.getEntityName() );
			entityBinding.setDiscriminatorMatchValue( discriminatorValue );
		}

		// single table and root entity
		// joined
		// table per class and non-abstract  entity
		else {
			Table includedTable = null;
			if ( superEntityBinding != null
					&& inheritanceType == InheritanceType.TABLE_PER_CLASS
					&& Table.class.isInstance( superEntityBinding.getPrimaryTable() ) ) {
				includedTable = Table.class.cast( superEntityBinding.getPrimaryTable() );
			}
			table = tableHelper.createTable(
					entitySource.getPrimaryTable(), new TableNamingStrategyHelper( entityBinding ), includedTable
			);
			tableName = table.getLogicalName().getText();
		}
		entityBinding.setPrimaryTable( table );
		entityBinding.setPrimaryTableName( tableName );
	}

	private void bindSecondaryTables(
			final EntityBinding entityBinding,
			final EntitySource entitySource) {
		for ( final SecondaryTableSource secondaryTableSource : entitySource.getSecondaryTables() ) {
			final TableSpecification table = tableHelper.createTable(
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
			final List<Column> targetColumns = foreignKeyHelper.determineForeignKeyTargetColumns( entityBinding, secondaryTableSource );
			final ForeignKey foreignKey = locateOrCreateForeignKey(
					secondaryTableSource.getExplicitForeignKeyName(),
					table,
					joinRelationalValueBindings,
					foreignKeyHelper.determineForeignKeyTargetTable( entityBinding, secondaryTableSource ),
					targetColumns
			);
			SecondaryTable secondaryTable = new SecondaryTable( table, foreignKey );
			if(secondaryTableSource.getFetchStyle()!=null)
			secondaryTable.setFetchStyle( secondaryTableSource.getFetchStyle() );
			secondaryTable.setInverse( secondaryTableSource.isInverse() );
			secondaryTable.setOptional( secondaryTableSource.isOptional() );
			secondaryTable.setCascadeDeleteEnabled( secondaryTableSource.isCascadeDeleteEnabled() );
			secondaryTable.setCustomDelete( secondaryTableSource.getCustomSqlDelete() );
			secondaryTable.setCustomInsert( secondaryTableSource.getCustomSqlInsert() );
			secondaryTable.setCustomUpdate( secondaryTableSource.getCustomSqlUpdate() );
			if ( secondaryTable.isCascadeDeleteEnabled() ) {
				foreignKey.setDeleteRule( ForeignKey.ReferentialAction.CASCADE );
			}
			entityBinding.addSecondaryTable( secondaryTable );
			metadata.addSecondaryTable( secondaryTable );
		}
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
		final ForeignKey foreignKey = foreignKeyHelper.locateOrCreateForeignKey(
				null,
				ownerTable,
				extractColumnsFromRelationalValueBindings( ownerAssociationAttributeBinding.getRelationalValueBindings() ),
				entityBinding.getPrimaryTable(),
				targetColumns
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
		if ( secondaryTable.isCascadeDeleteEnabled() ) {
			foreignKey.setDeleteRule( ForeignKey.ReferentialAction.CASCADE );
		}
		entityBinding.addSecondaryTable( secondaryTable );
		metadata.addSecondaryTable( secondaryTable );
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
						bindingContext().getNamingStrategy().joinKeyColumnName(
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
				throw bindingContext().makeMappingException(
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
					column = tableHelper.locateOrCreateColumn(
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
		typeHelper.bindJdbcDataType(
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
			final List<Column> targetColumns) {
		return foreignKeyHelper.locateOrCreateForeignKey(
				foreignKeyName,
				sourceTable,
				extractColumnsFromRelationalValueBindings( sourceRelationalValueBindings ),
				targetTable,
				targetColumns
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
		properties.putAll( metadata.getServiceRegistry().getService( ConfigurationService.class ).getSettings() );
		if ( !properties.contains( AvailableSettings.PREFER_POOLED_VALUES_LO ) ) {
			properties.put( AvailableSettings.PREFER_POOLED_VALUES_LO, "false" );
		}
		if ( !properties.contains( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER ) ) {
			properties.put(
					PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
					metadata.getObjectNameNormalizer()
			);
		}
		final EntityIdentifier entityIdentifier = rootEntityBinding.getHierarchyDetails().getEntityIdentifier();
		entityIdentifier.createIdentifierGenerator( identifierGeneratorFactory, properties );
		if ( IdentityGenerator.class.isInstance( entityIdentifier.getIdentifierGenerator() ) ) {
			if ( rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan() != 1 ) {
				throw bindingContext().makeMappingException(
						String.format(
								"ID for %s is mapped as an identity with %d columns. IDs mapped as an identity can only have 1 column.",
								rootEntityBinding.getEntity().getName(),
								rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan()
						)
				);
			}
			rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 ).setIdentity( true );
		}
		if ( PersistentIdentifierGenerator.class.isInstance( entityIdentifier.getIdentifierGenerator() ) ) {
			( (PersistentIdentifierGenerator) entityIdentifier.getIdentifierGenerator() ).registerExportables( metadata.getDatabase() );
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
		return attributeSource.isSingular() ?
				bindSingularAttribute(
						attributeBindingContainer,
						SingularAttributeSource.class.cast( attributeSource ),
						false
				) :
				bindPluralAttribute( attributeBindingContainer, PluralAttributeSource.class.cast( attributeSource ) );
	}

	private BasicAttributeBinding bindBasicAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource,
			SingularAttribute attribute) {
		if ( attribute == null ) {
			attribute = createSingularAttribute( attributeBindingContainer, attributeSource );
		}
		final List<RelationalValueBinding> relationalValueBindings =
				relationalValueBindingHelper.createRelationalValueBindings(
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
		typeHelper.bindSingularAttributeType(
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
		typeHelper.bindAggregatedCompositeAttributeType(
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
		final ValueHolder<Class<?>> defaultJavaClassReference;
		if ( attribute == null ) {
			if ( attributeSource.getClassName() != null ) {
				composite = new Aggregate(
						attributeSource.getPath(),
						attributeSource.getClassName(),
						attributeSource.getClassReference() != null ?
								attributeSource.getClassReference() :
								bindingContext().makeClassReference( attributeSource.getClassName() ),
						null
				);
				// no need for a default because there's an explicit class name provided
			}
			else {
				defaultJavaClassReference = createSingularAttributeJavaType(
						attributeBindingContainer.getClassReference(), attributeSource.getName()
				);
				composite = new Aggregate(
						attributeSource.getPath(),
						defaultJavaClassReference.getValue().getName(),
						defaultJavaClassReference,
						null
				);
			}
			attribute = attributeBindingContainer.getAttributeContainer().createCompositeAttribute(
					attributeSource.getName(),
					composite
			);
		}
		else {
			composite = (Aggregate) attribute.getSingularAttributeType();
		}

		final SingularAttribute referencingAttribute =
				StringHelper.isEmpty( attributeSource.getParentReferenceAttributeName() ) ?
						null :
						composite.createSingularAttribute( attributeSource.getParentReferenceAttributeName() );
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
			Class<? extends ComponentTuplizer> tuplizerClass = bindingContext().getServiceRegistry()
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

		final ValueHolder<Class<?>> referencedEntityJavaTypeValue = createSingularAttributeJavaType( attribute );
		final EntityBinding referencedEntityBinding = locateEntityBinding(
				referencedEntityJavaTypeValue,
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
				relationalValueBindings.isEmpty() || relationalValueBindingHelper.hasDerivedValue( relationalValueBindings ) ?
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

		final SingularNonAssociationAttributeBinding idAttributeBinding =
				referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		final String uniqueKeyAttributeName = idAttributeBinding == referencedAttributeBinding ?
				null :
				getRelativePathFromEntityName( referencedAttributeBinding );

		final Type resolvedType = metadata.getTypeResolver().getTypeFactory().manyToOne(
				referencedEntityBinding.getEntity().getName(),
				uniqueKeyAttributeName == null,
				uniqueKeyAttributeName,
				attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
				attributeSource.isUnWrapProxy(),
				!attributeSource.isNotFoundAnException(),
				attributeSource.isUnique()
		);
		typeHelper.bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				attributeSource.getTypeInformation(),
				referencedEntityJavaTypeValue.getValue().getName(),
				resolvedType
		);

		typeHelper.bindJdbcDataType(
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

		final ValueHolder<Class<?>> referencedEntityJavaTypeValue = createSingularAttributeJavaType( attribute );
		final EntityBinding referencedEntityBinding = locateEntityBinding(
				referencedEntityJavaTypeValue,
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
			resolvedType = metadata.getTypeResolver().getTypeFactory().oneToOne(
					referencedEntityBinding.getEntity().getName(),
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
			resolvedType = metadata.getTypeResolver().getTypeFactory().specialOneToOne(
					referencedEntityBinding.getEntity().getName(),
					attributeSource.getForeignKeyDirection(),
					uniqueKeyAttributeName == null,
					uniqueKeyAttributeName,
					attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
					attributeSource.isUnWrapProxy(),
					attributeBindingContainer.seekEntityBinding().getEntityName(),
					attribute.getName()
			);
		}

		typeHelper.bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				attributeSource.getTypeInformation(),
				referencedEntityJavaTypeValue.getValue().getName(),
				resolvedType
		);
		if ( !attributeBinding.getRelationalValueBindings().isEmpty() ) {
			typeHelper.bindJdbcDataType(
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
		attributeBinding.setCaching( attributeSource.getCaching() );
		if ( StringHelper.isNotEmpty( attributeSource.getCustomPersisterClassName() ) ) {
			attributeBinding.setExplicitPersisterClass(
					bindingContext().<CollectionPersister>locateClassByName(
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
		ReflectedCollectionJavaTypes reflectedCollectionJavaTypes = HibernateTypeHelper.getReflectedCollectionJavaTypes(
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
				throw bindingContext().makeMappingException(
						String.format(
								"Unknown type of collection element: %s",
								attributeSource.getElementSource().getNature()
						)
				);
		}
		// Cannot resolve plural attribute type until after the element binding is bound.
		final Type resolvedType = typeHelper.resolvePluralType( attributeBinding, attributeSource, nature );
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		typeHelper.bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				attributeSource.getTypeInformation(),
				HibernateTypeHelper.defaultCollectionJavaTypeName( reflectedCollectionJavaTypes, attributeSource ),
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
		metadata.addCollection( attributeBinding );
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
			final String defaultElementJavaTypeName) {
		bindBasicPluralElementRelationalValues( elementSource, elementBinding );
		typeHelper.bindBasicCollectionElementType( elementBinding, elementSource, defaultElementJavaTypeName );
		elementBinding.getPluralAttributeBinding().getAttribute().setElementType(
				bindingContext().makeJavaType( elementBinding.getHibernateTypeDescriptor().getJavaTypeName() )
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
		TableSpecification collectionTable = tableHelper.createTable(
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
			final String defaultElementJavaTypeName) {
		final PluralAttributeBinding pluralAttributeBinding = elementBinding.getPluralAttributeBinding();
		ValueHolder<Class<?>> defaultElementJavaClassReference = null;
		// Create the aggregate type
		// TODO: aggregateName should be set to elementSource.getPath() (which is currently not implemented)
		//       or Binder should define AttributeBindingContainer paths instead.
		String aggregateName = pluralAttributeBinding.getAttribute().getRole() + ".element";
		final Aggregate aggregate;
		if ( elementSource.getClassName() != null ) {
			aggregate = new Aggregate(
					aggregateName,
					elementSource.getClassName(),
					elementSource.getClassReference() != null ?
							elementSource.getClassReference() :
							bindingContext().makeClassReference( elementSource.getClassName() ),
					null
			);
		}
		else {
			defaultElementJavaClassReference = bindingContext().makeClassReference( defaultElementJavaTypeName );
			aggregate = new Aggregate(
					aggregateName,
					defaultElementJavaClassReference.getValue().getName(),
					defaultElementJavaClassReference,
					null
			);
		}
		final SingularAttribute parentAttribute =
				StringHelper.isEmpty( elementSource.getParentReferenceAttributeName() ) ?
						null :
						aggregate.createSingularAttribute( elementSource.getParentReferenceAttributeName() );
		final CompositeAttributeBindingContainer compositeAttributeBindingContainer =
				elementBinding.createCompositeAttributeBindingContainer(
						aggregate,
						createMetaAttributeContext(
								pluralAttributeBinding.getContainer(),
								elementSource.getMetaAttributeSources()
						),
						parentAttribute
				);

		bindAttributes( compositeAttributeBindingContainer, elementSource );
		pluralAttributeBinding.getAttribute().setElementType( aggregate );
		Type resolvedType = metadata.getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel( compositeAttributeBindingContainer, false, false )
		);
		// TODO: binding the HibernateTypeDescriptor should be simplified since we know the class name already
		typeHelper.bindHibernateTypeDescriptor(
				elementBinding.getHibernateTypeDescriptor(),
				aggregate.getClassName(),
				null,
				defaultElementJavaClassReference == null ? null : defaultElementJavaClassReference.getValue().getName(),
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
			final String defaultIndexJavaTypeName) {
		throw new NotYetImplementedException( "Plural attribute index that is an attribute of the referenced entity is not supported yet." );
	}

	private void bindBasicCollectionIndex(
			final IndexedPluralAttributeBinding attributeBinding,
			final BasicPluralAttributeIndexSource indexSource,
			final String defaultIndexJavaTypeName) {
		final BasicPluralAttributeIndexBinding indexBinding =
				(BasicPluralAttributeIndexBinding) attributeBinding.getPluralAttributeIndexBinding();
		// TODO: need to resolve default column names.
		indexBinding.setRelationalValueBindings(
				relationalValueBindingHelper.createRelationalValueBindings(
						attributeBinding.getContainer(),
						indexSource,
						attributeBinding.getPluralAttributeKeyBinding().getCollectionTable(),
						indexSource.getDefaultNamingStrategies(),
						attributeBinding.getPluralAttributeElementBinding()
								.getNature() != PluralAttributeElementBinding.Nature.ONE_TO_MANY
				)
		);
		// TODO: create a foreign key if non-inverse and the index is an association

		typeHelper.bindHibernateTypeDescriptor(
				indexBinding.getHibernateTypeDescriptor(),
				indexSource.getTypeInformation(),
				defaultIndexJavaTypeName
		);
		typeHelper.bindJdbcDataType(
				indexBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				indexBinding.getRelationalValueBindings()
		);
		IndexedPluralAttribute indexedPluralAttribute =
				(IndexedPluralAttribute) indexBinding.getIndexedPluralAttributeBinding().getAttribute();
		indexedPluralAttribute.setIndexType(
				bindingContext().makeJavaType( indexBinding.getHibernateTypeDescriptor().getJavaTypeName() )
		);
	}

	private void bindCompositeCollectionIndex(
		final CompositePluralAttributeIndexBinding indexBinding,
		final IndexedPluralAttributeSource indexedPluralAttributeSource,
		final String defaultIndexJavaTypeName) {
		final PluralAttributeBinding pluralAttributeBinding = indexBinding.getIndexedPluralAttributeBinding();
		final CompositePluralAttributeIndexSource indexSource =
				(CompositePluralAttributeIndexSource) indexedPluralAttributeSource.getIndexSource();
		ValueHolder<Class<?>> defaultElementJavaClassReference = null;
		// Create the aggregate type
		// TODO: aggregateName should be set to elementSource.getPath() (which is currently not implemented)
		//       or Binder should define AttributeBindingContainer paths instead.
		String aggregateName = pluralAttributeBinding.getAttribute().getRole() + ".index";
		final Aggregate aggregate;
		if ( indexSource.getClassName() != null ) {
			aggregate = new Aggregate(
					aggregateName,
					indexSource.getClassName(),
					indexSource.getClassReference() != null ?
							indexSource.getClassReference() :
							bindingContext().makeClassReference( indexSource.getClassName() ),
					null
			);
		}
		else {
			defaultElementJavaClassReference = bindingContext().makeClassReference( defaultIndexJavaTypeName );
			aggregate = new Aggregate(
					aggregateName,
					defaultElementJavaClassReference.getValue().getName(),
					defaultElementJavaClassReference,
					null
			);
		}
		final CompositeAttributeBindingContainer compositeAttributeBindingContainer =
				indexBinding.createCompositeAttributeBindingContainer(
						aggregate,
						null,
						null
				);

		bindAttributes( compositeAttributeBindingContainer, indexSource );

		Type resolvedType = metadata.getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel( compositeAttributeBindingContainer, false, false )
		);
		// TODO: binding the HibernateTypeDescriptor should be simplified since we know the class name already
		typeHelper.bindHibernateTypeDescriptor(
				indexBinding.getHibernateTypeDescriptor(),
				aggregate.getClassName(),
				null,
				defaultElementJavaClassReference == null ? null : defaultElementJavaClassReference.getValue().getName(),
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
			final String defaultElementJavaTypeName) {
		throwExceptionIfNotFoundIgnored( elementSource.isNotFoundAnException() );
		elementBinding.setElementEntityIdentifier(
				referencedEntityBinding.getKeyRelationalValueBindings()
		);

		Type resolvedElementType = metadata.getTypeResolver().getTypeFactory().manyToOne(
				referencedEntityBinding.getEntity().getName(),
				true,
				null,
				false,
				false,
				!elementSource.isNotFoundAnException(), //TODO: should be attributeBinding.isIgnoreNotFound(),
				false
		);
		final HibernateTypeDescriptor hibernateTypeDescriptor = elementBinding.getHibernateTypeDescriptor();
		typeHelper.bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				referencedEntityBinding.getEntity().getName(),
				null,
				defaultElementJavaTypeName,
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
			final String defaultElementJavaTypeName) {
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
				!relationalValueBindingHelper.hasDerivedValue( relationalValueBindings ) ) {
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
			tableHelper.createUniqueKey( collectionTable, columns, null );
		}
		elementBinding.setJoinRelationalValueBindings( relationalValueBindings, foreignKey );
		typeHelper.bindManyToManyAttributeType(
				elementBinding,
				elementSource,
				referencedEntityBinding,
				defaultElementJavaTypeName
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
		final String defaultCollectionIndexJavaTypeName =
				HibernateTypeHelper.defaultCollectionIndexJavaTypeName( reflectedCollectionJavaTypes );
		final PluralAttributeIndexSource indexSource = attributeSource.getIndexSource();
		if ( indexSource.isReferencedEntityAttribute() ) {
			bindEntityAttributePluralAttributeIndex(
					attributeBinding,
					(EntityAttributePluralAttributeIndexSource) indexSource,
					defaultCollectionIndexJavaTypeName
			);
		}
		else {
			switch ( attributeSource.getIndexSource().getNature() ) {
				case BASIC: {
					bindBasicCollectionIndex(
							attributeBinding,
							(BasicPluralAttributeIndexSource) attributeSource.getIndexSource(),
							defaultCollectionIndexJavaTypeName
					);
					break;
				}
				case AGGREGATE: {
					bindCompositeCollectionIndex(
							(CompositePluralAttributeIndexBinding) attributeBinding.getPluralAttributeIndexBinding(),
							attributeSource,
							defaultCollectionIndexJavaTypeName
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
				HibernateTypeHelper.defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes )
		);
	}

	private void bindManyToManyAttribute(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final ManyToManyPluralAttributeElementSource elementSource =
				(ManyToManyPluralAttributeElementSource) attributeSource.getElementSource();
		final String defaultElementJavaTypeName = HibernateTypeHelper.defaultCollectionElementJavaTypeName(
				reflectedCollectionJavaTypes
		);
		String referencedEntityName =
				elementSource.getReferencedEntityName() != null ?
						elementSource.getReferencedEntityName() :
						defaultElementJavaTypeName;
		if ( referencedEntityName == null ) {
			throw bindingContext().makeMappingException(
					String.format(
							"The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
							createAttributePathQualifiedByEntityName( attributeBinding )
					)
			);
		}
		EntityBinding referencedEntityBinding = locateEntityBinding( referencedEntityName );
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
				defaultElementJavaTypeName
		);
	}

	private void bindOneToManyAttribute(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding attributeBinding,
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final OneToManyPluralAttributeElementSource elementSource =
				(OneToManyPluralAttributeElementSource) attributeSource.getElementSource();
		final String defaultElementJavaTypeName = HibernateTypeHelper.defaultCollectionElementJavaTypeName(
				reflectedCollectionJavaTypes
		);
		String referencedEntityName =
				elementSource.getReferencedEntityName() != null ?
						elementSource.getReferencedEntityName() :
						defaultElementJavaTypeName;
		if ( referencedEntityName == null ) {
			throw bindingContext().makeMappingException(
					String.format(
							"The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
							createAttributePathQualifiedByEntityName( attributeBinding )
					)
			);
		}
		EntityBinding referencedEntityBinding = locateEntityBinding( referencedEntityName );
		bindOneToManyCollectionKey( attributeBinding, attributeSource, referencedEntityBinding );
		bindOneToManyCollectionElement(
				(OneToManyPluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
				(OneToManyPluralAttributeElementSource) attributeSource.getElementSource(),
				referencedEntityBinding,
				defaultElementJavaTypeName
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
				HibernateTypeHelper.defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes )
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
						bindingContext().locateClassByName( sortable.getComparatorName() );
				try {
					attributeBinding.setComparator( comparatorClass.newInstance() );
				}
				catch ( Exception error ) {
					throw bindingContext().makeMappingException(
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
				relationalValueBindingHelper.createRelationalValueBindings(
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
		typeHelper.bindJdbcDataType( resolvedKeyType, keyBinding.getRelationalValueBindings() );
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
				? bindingContext().getMappingDefaults().getPropertyAccessorName()
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
		if ( relationalValueBindingHelper.hasDerivedValue( indexBinding.getRelationalValueBindings() ) ) {
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
		return createMetaAttributeContext( attributeBindingContainer, attributeSource.getMetaAttributeSources() );
	}

	private static MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final Iterable<? extends MetaAttributeSource> metaAttributeSources) {
		return createMetaAttributeContext(
				metaAttributeSources,
				false,
				attributeBindingContainer.getMetaAttributeContext()
		);
	}

	private static MetaAttributeContext createMetaAttributeContext(
			final Iterable<? extends MetaAttributeSource> metaAttributeSources,
			final boolean onlyInheritable,
			final MetaAttributeContext parentContext) {
		final MetaAttributeContext subContext = new MetaAttributeContext( parentContext );
		for ( final MetaAttributeSource metaAttributeSource : metaAttributeSources ) {
			if ( onlyInheritable && !metaAttributeSource.isInheritable() ) {
				continue;
			}
			final String name = metaAttributeSource.getName();
			MetaAttribute metaAttribute = subContext.getLocalMetaAttribute( name );
			if ( metaAttribute == null || metaAttribute == parentContext.getMetaAttribute( name ) ) {
				metaAttribute = new MetaAttribute( name );
				subContext.add( metaAttribute );
			}
			metaAttribute.addValue( metaAttributeSource.getValue() );
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

	private static ValueHolder<Class<?>> createSingularAttributeJavaType(
			final Class<?> attributeContainerClassReference,
			final String attributeName) {
		ValueHolder.DeferredInitializer<Class<?>> deferredInitializer =
				new ValueHolder.DeferredInitializer<Class<?>>() {
					@Override
					public Class<?> initialize() {
						return ReflectHelper.reflectedPropertyClass(
								attributeContainerClassReference,
								attributeName
						);
					}
				};
		return new ValueHolder<Class<?>>( deferredInitializer );
	}

	private static ValueHolder<Class<?>> createSingularAttributeJavaType(
			final SingularAttribute attribute) {
		return createSingularAttributeJavaType(
				attribute.getAttributeContainer().getClassReference(),
				attribute.getName()
		);
	}

	public static interface DefaultNamingStrategy {

		String defaultName(NamingStrategy namingStrategy);
	}
}
