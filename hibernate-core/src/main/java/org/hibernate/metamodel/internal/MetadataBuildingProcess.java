/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.DuplicateMappingException;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SyntheticAttributeHelper;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.MetadataSourceProcessingOrder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.internal.source.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.metamodel.internal.source.hbm.HbmMetadataSourceProcessorImpl;
import org.hibernate.metamodel.reflite.internal.RepositoryImpl;
import org.hibernate.metamodel.reflite.spi.Repository;
import org.hibernate.metamodel.source.internal.jandex.Unifier;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.spi.AdditionalJaxbRootProducer;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.InFlightMetadataCollector;
import org.hibernate.metamodel.spi.MetadataBuildingOptions;
import org.hibernate.metamodel.spi.MetadataContributor;
import org.hibernate.metamodel.spi.MetadataSourceProcessor;
import org.hibernate.metamodel.spi.TypeContributions;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BackRefAttributeBinding;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.domain.BasicType;
import org.hibernate.metamodel.spi.domain.JavaClassReference;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.FilterParameterSource;
import org.hibernate.metamodel.spi.source.IdentifierGeneratorSource;
import org.hibernate.metamodel.spi.source.MappingDefaults;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.TypeResolver;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.xml.spi.BindResult;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

/**
 * Represents the process of building a Metadata object.  The main entry point is the
 * static {@link #build}
 *
 * @author Steve Ebersole
 */
public class MetadataBuildingProcess {
	private static final Logger log = Logger.getLogger( MetadataBuildingProcess.class );

	public static MetadataImpl build(MetadataSources sources, MetadataBuildingOptions options) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// preliminary phases
		final IndexView jandexView = handleJandex( options, sources );
		final BasicTypeRegistry basicTypeRegistry = handleTypes( options );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// prep to start handling binding in earnest
		final MappingDefaultsImpl mappingDefaults = new MappingDefaultsImpl( options );
		final Repository typeRepository = new RepositoryImpl(
				options.getTempClassLoader(),
				options.getServiceRegistry()
		);
		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(
				options,
				new TypeResolver( basicTypeRegistry, new TypeFactory() )
		);
		final RootBindingContextImpl rootBindingContext = new RootBindingContextImpl(
				options,
				mappingDefaults,
				typeRepository,
				metadataCollector
		);


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Set up the processors and start binding
		//		NOTE : this becomes even more simplified after we move purely
		// 		to unified model

		final MetadataSourceProcessor[] metadataSourceProcessors;
		if ( options.getMetadataSourceProcessingOrder() == MetadataSourceProcessingOrder.HBM_FIRST ) {
			metadataSourceProcessors = new MetadataSourceProcessor[] {
					new HbmMetadataSourceProcessorImpl( rootBindingContext, sources ),
					new AnnotationMetadataSourceProcessorImpl( rootBindingContext, jandexView )
			};
		}
		else {
			metadataSourceProcessors = new MetadataSourceProcessor[] {
					new AnnotationMetadataSourceProcessorImpl( rootBindingContext, jandexView ),
					new HbmMetadataSourceProcessorImpl( rootBindingContext, sources )
			};
		}

		processTypeDefinitions( metadataSourceProcessors, rootBindingContext );
		processFilterDefinitions( metadataSourceProcessors, rootBindingContext );
		processIdentifierGenerators( metadataSourceProcessors, rootBindingContext );
		processMappings( metadataSourceProcessors, rootBindingContext );
		bindMappingDependentMetadata( metadataSourceProcessors, rootBindingContext );

		final ClassLoaderService classLoaderService = options.getServiceRegistry().getService( ClassLoaderService.class );

		for ( MetadataContributor contributor : classLoaderService.loadJavaServices( MetadataContributor.class ) ) {
			contributor.contribute( metadataCollector, jandexView );
		}

		final List<BindResult> bindResults = new ArrayList<BindResult>();
		for ( AdditionalJaxbRootProducer producer : classLoaderService.loadJavaServices( AdditionalJaxbRootProducer.class ) ) {
			bindResults.addAll( producer.produceRoots( metadataCollector, jandexView ) );
		}
		final HbmMetadataSourceProcessorImpl processor = new HbmMetadataSourceProcessorImpl( rootBindingContext, bindResults );
		final Binder binder = new Binder( rootBindingContext );
		binder.addEntityHierarchies( processor.extractEntityHierarchies() );
		binder.bindEntityHierarchies();

		secondPass( rootBindingContext );

		return metadataCollector.buildMetadataInstance();
	}

	private static IndexView handleJandex(MetadataBuildingOptions options, MetadataSources sources) {
		final ConfigurationService configurationService = options.getServiceRegistry().getService( ConfigurationService.class );

		final IndexView baseJandexIndex;
		if ( options.getJandexView() != null ) {
			baseJandexIndex = options.getJandexView();
		}
		else {
			final boolean autoIndexMemberTypes = configurationService.getSetting(
					AvailableSettings.ENABLE_AUTO_INDEX_MEMBER_TYPES,
					StandardConverters.BOOLEAN,
					false
			);
			baseJandexIndex = sources.buildJandexView( autoIndexMemberTypes );
		}

		final List<BindResult<JaxbEntityMappings>> jpaXmlBindings = new ArrayList<BindResult<JaxbEntityMappings>>();
		for ( BindResult bindResult : sources.getBindResultList() ) {
			if ( JaxbEntityMappings.class.isInstance( bindResult.getRoot() ) ) {
				// todo : this will be checked after hbm transformation is in place.
				//noinspection unchecked
				jpaXmlBindings.add( bindResult );
			}
		}

		return Unifier.unify( baseJandexIndex, jpaXmlBindings, options.getServiceRegistry() );
	}

	private static BasicTypeRegistry handleTypes(MetadataBuildingOptions options) {
		final ClassLoaderService classLoaderService = options.getServiceRegistry().getService( ClassLoaderService.class );

		// ultimately this needs to change a little bit to account for HHH-7792
		final BasicTypeRegistry basicTypeRegistry = new BasicTypeRegistry();

		final TypeContributions typeContributions = new TypeContributions() {
			@Override
			public void contributeType(org.hibernate.type.BasicType type) {
				basicTypeRegistry.register( type );
			}

			@Override
			public void contributeType(UserType type, String[] keys) {
				basicTypeRegistry.register( type, keys );
			}

			@Override
			public void contributeType(CompositeUserType type, String[] keys) {
				basicTypeRegistry.register( type, keys );
			}
		};

		// add Dialect contributed types
		final Dialect dialect = options.getServiceRegistry().getService( JdbcServices.class ).getDialect();
		dialect.contributeTypes( typeContributions, options.getServiceRegistry() );

		// add TypeContributor contributed types.
		for ( TypeContributor contributor : classLoaderService.loadJavaServices( TypeContributor.class ) ) {
			contributor.contribute( typeContributions, options.getServiceRegistry() );
		}

		// add explicit application registered types
		for ( org.hibernate.type.BasicType basicType : options.getBasicTypeRegistrations() ) {
			basicTypeRegistry.register( basicType );
		}

		return basicTypeRegistry;
	}

	private static void processTypeDefinitions(
			MetadataSourceProcessor[] metadataSourceProcessors,
			RootBindingContextImpl bindingContext) {
		final ClassLoaderService cls = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );

		for ( MetadataSourceProcessor processor : metadataSourceProcessors ) {
			for ( TypeDescriptorSource typeDescriptorSource : processor.extractTypeDefinitionSources() ) {
				bindingContext.getMetadataCollector().addTypeDefinition(
						new TypeDefinition(
								typeDescriptorSource.getName(),
								cls.classForName( typeDescriptorSource.getTypeImplementationClassName() ),
								typeDescriptorSource.getRegistrationKeys(),
								typeDescriptorSource.getParameters()
						)
				);
			}
		}
	}

	private static void processFilterDefinitions(
			MetadataSourceProcessor[] metadataSourceProcessors,
			RootBindingContextImpl bindingContext) {
		for ( MetadataSourceProcessor processor : metadataSourceProcessors ) {
			for ( FilterDefinitionSource filterDefinitionSource : processor.extractFilterDefinitionSources() ) {
				bindingContext.getMetadataCollector().addFilterDefinition(
						new FilterDefinition(
								filterDefinitionSource.getName(),
								filterDefinitionSource.getCondition(),
								resolveFilterDefinitionParamType(
										filterDefinitionSource.getParameterSources(),
										bindingContext
								)
						)
				);
			}
		}
	}

	private static Map<String, org.hibernate.type.Type> resolveFilterDefinitionParamType(
			Iterable<FilterParameterSource> filterParameterSources,
			RootBindingContextImpl bindingContext){
		if ( CollectionHelper.isEmpty( filterParameterSources ) ) {
			return Collections.emptyMap();
		}

		Map<String, org.hibernate.type.Type> params = new HashMap<String, org.hibernate.type.Type>(  );
		for(final FilterParameterSource parameterSource : filterParameterSources){
			final String name = parameterSource.getParameterName();
			final String typeName = parameterSource.getParameterValueTypeName();
			final org.hibernate.type.Type type = bindingContext.getMetadataCollector().getTypeResolver().heuristicType(
					typeName
			);
			params.put( name, type );
		}
		return params;
	}

	private static void processIdentifierGenerators(
			MetadataSourceProcessor[] metadataSourceProcessors,
			RootBindingContextImpl bindingContext) {
		for ( MetadataSourceProcessor processor : metadataSourceProcessors ) {
			for ( IdentifierGeneratorSource identifierGeneratorSource : processor.extractGlobalIdentifierGeneratorSources() ) {
				bindingContext.getMetadataCollector().addIdGenerator(
						new IdentifierGeneratorDefinition(
								identifierGeneratorSource.getGeneratorName(),
								identifierGeneratorSource.getGeneratorImplementationName(),
								identifierGeneratorSource.getParameters()
						)
				);
			}
		}
	}

	private static void processMappings(
			MetadataSourceProcessor[] metadataSourceProcessors,
			RootBindingContextImpl bindingContext) {
		final Binder binder = new Binder( bindingContext );
		// Add all hierarchies first, before binding.
		for ( MetadataSourceProcessor processor : metadataSourceProcessors ) {
			binder.addEntityHierarchies( processor.extractEntityHierarchies() );
		}
		binder.bindEntityHierarchies();
	}

	private static void bindMappingDependentMetadata(
			MetadataSourceProcessor[] metadataSourceProcessors,
			RootBindingContextImpl bindingContext) {
		// Create required back references, which are required for one-to-many associations with key bindings that are non-inverse,
		// non-nullable, and unidirectional
		for ( PluralAttributeBinding pluralAttributeBinding : bindingContext.getMetadataCollector().getCollectionBindings() ) {
			// Find one-to-many associations with key bindings that are non-inverse and non-nullable
			PluralAttributeKeyBinding keyBinding = pluralAttributeBinding.getPluralAttributeKeyBinding();
			if ( keyBinding.isInverse() || keyBinding.isNullable() ||
					pluralAttributeBinding.getPluralAttributeElementBinding().getNature() !=
							PluralAttributeElementBinding.Nature.ONE_TO_MANY ) {
				continue;
			}
			// Ensure this isn't a bidirectional association by ensuring FK columns don't match relational columns of any
			// many-to-one on opposite side
			EntityBinding referencedEntityBinding = bindingContext.getMetadataCollector().getEntityBinding(
					pluralAttributeBinding.getPluralAttributeElementBinding()
							.getHibernateTypeDescriptor()
							.getResolvedTypeMapping()
							.getName()
			);
			List<RelationalValueBinding> keyValueBindings = keyBinding.getRelationalValueBindings();
			boolean bidirectional = false;
			for ( AttributeBinding attributeBinding : referencedEntityBinding.attributeBindings() ) {
				if ( !(attributeBinding instanceof ManyToOneAttributeBinding ) ) {
					continue;
				}
				// Check if the opposite many-to-one attribute binding references the one-to-many attribute binding being processed
				ManyToOneAttributeBinding manyToOneAttributeBinding = ( ManyToOneAttributeBinding ) attributeBinding;
				if ( !manyToOneAttributeBinding.getReferencedEntityBinding().equals(
						pluralAttributeBinding.getContainer().seekEntityBinding() ) ) {
					continue;
				}
				// Check if the many-to-one attribute binding's columns match the one-to-many attribute binding's FK columns
				// (meaning this is a bidirectional association, and no back reference should be created)
				List<RelationalValueBinding> valueBindings = manyToOneAttributeBinding.getRelationalValueBindings();
				if ( keyValueBindings.size() != valueBindings.size() ) {
					continue;
				}
				bidirectional = true;
				for ( int ndx = valueBindings.size(); --ndx >= 0; ) {
					if ( keyValueBindings.get(ndx) != valueBindings.get( ndx ) ) {
						bidirectional = false;
						break;
					}
				}
				if ( bidirectional ) {
					break;
				}
			}
			if ( bidirectional ) continue;

			// Create the synthetic back reference attribute
			SingularAttribute syntheticAttribute =
					referencedEntityBinding.getEntity().createSyntheticSingularAttribute(
							SyntheticAttributeHelper.createBackRefAttributeName(
									pluralAttributeBinding.getAttribute()
											.getRole()
							) );
			// Create the back reference attribute binding.
			BackRefAttributeBinding backRefAttributeBinding = referencedEntityBinding.makeBackRefAttributeBinding(
					syntheticAttribute, pluralAttributeBinding, false
			);
			backRefAttributeBinding.getHibernateTypeDescriptor().copyFrom( keyBinding.getHibernateTypeDescriptor() );
			backRefAttributeBinding.getAttribute().resolveType(
					keyBinding.getReferencedAttributeBinding().getAttribute().getSingularAttributeType() );
			if ( pluralAttributeBinding.hasIndex() ) {
				SingularAttribute syntheticIndexAttribute =
						referencedEntityBinding.getEntity().createSyntheticSingularAttribute(
								SyntheticAttributeHelper.createIndexBackRefAttributeName( pluralAttributeBinding.getAttribute().getRole() ) );
				BackRefAttributeBinding indexBackRefAttributeBinding = referencedEntityBinding.makeBackRefAttributeBinding(
						syntheticIndexAttribute, pluralAttributeBinding, true
				);
				final PluralAttributeIndexBinding indexBinding =
						( (IndexedPluralAttributeBinding) pluralAttributeBinding ).getPluralAttributeIndexBinding();
				indexBackRefAttributeBinding.getHibernateTypeDescriptor().copyFrom(
						indexBinding.getHibernateTypeDescriptor()
				);
				indexBackRefAttributeBinding.getAttribute().resolveType(
						indexBinding.getPluralAttributeIndexType()
				);
			}
		}

		for ( MetadataSourceProcessor metadataSourceProcessor : metadataSourceProcessors ) {
			metadataSourceProcessor.processMappingDependentMetadata();
		}
	}

	private static void secondPass(RootBindingContextImpl bindingContext) {
		// This must be done outside of Table, rather than statically, to ensure
		// deterministic alias names.  See HHH-2448.
		int uniqueInteger = 0;
		for ( Schema schema : bindingContext.getMetadataCollector().getDatabase().getSchemas() ) {
			for ( Table table : schema.getTables() ) {
				table.setTableNumber( uniqueInteger++ );
			}
		}


		if ( bindingContext.getBuildingOptions().getCacheRegionDefinitions() == null
				|| bindingContext.getBuildingOptions().getCacheRegionDefinitions().isEmpty() ) {
			return;
		}

		for ( CacheRegionDefinition cacheRegionDefinition : bindingContext.getBuildingOptions().getCacheRegionDefinitions() ) {
			final String role = cacheRegionDefinition.getRole();
			if ( cacheRegionDefinition.getRegionType() == CacheRegionDefinition.CacheRegionType.ENTITY ) {
				final EntityBinding entityBinding = bindingContext.getMetadataCollector().getEntityBinding( role );
				if ( entityBinding != null ) {
					entityBinding.getHierarchyDetails().setCaching(
							new Caching(
									cacheRegionDefinition.getRegion(),
									AccessType.fromExternalName( cacheRegionDefinition.getUsage() ),
									cacheRegionDefinition.isCacheLazy()
							)
					);
				}
				else {
					//logging?
					throw new MappingException( "Can't find entitybinding for role " + role +" to apply cache configuration" );
				}

			}
			else if ( cacheRegionDefinition.getRegionType() == CacheRegionDefinition.CacheRegionType.COLLECTION ) {
				final PluralAttributeBinding pluralAttributeBinding = bindingContext.getMetadataCollector().getCollection(
						role
				);
				if ( pluralAttributeBinding != null ) {
					AbstractPluralAttributeBinding.class.cast( pluralAttributeBinding ).setCaching(
							new Caching(
									cacheRegionDefinition.getRegion(),
									AccessType.fromExternalName( cacheRegionDefinition.getUsage() ),
									cacheRegionDefinition.isCacheLazy()
							)
					);
				}
				else {
					//logging?
					throw new MappingException( "Can't find entitybinding for role " + role +" to apply cache configuration" );
				}
			}
		}

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BindingContext impl

	public static class RootBindingContextImpl implements BindingContext {
		private final MetadataBuildingOptions options;
		private final MappingDefaults mappingDefaults;
		private final Repository typeRepository;
		private final InFlightMetadataCollectorImpl metadataCollector;
		private final MetaAttributeContext globalMetaAttributeContext = new MetaAttributeContext();

		private final ClassLoaderService classLoaderService;

		public RootBindingContextImpl(
				MetadataBuildingOptions options,
				MappingDefaults mappingDefaults,
				Repository typeRepository,
				InFlightMetadataCollectorImpl metadataCollector) {
			this.options = options;
			this.mappingDefaults = mappingDefaults;
			this.typeRepository = typeRepository;
			this.metadataCollector = metadataCollector;

			this.classLoaderService = options.getServiceRegistry().getService( ClassLoaderService.class );
		}

		@Override
		public MetadataBuildingOptions getBuildingOptions() {
			return options;
		}

		@Override
		public MappingDefaults getMappingDefaults() {
			return mappingDefaults;
		}

		@Override
		public Repository getRefliteRepository() {
			return typeRepository;
		}

		@Override
		public String qualifyClassName(String name) {
			// see comments on MappingDefaultsImpl.getPackageName()
			return name;
		}

		@Override
		public InFlightMetadataCollector getMetadataCollector() {
			return metadataCollector;
		}

		@Override
		public MetaAttributeContext getGlobalMetaAttributeContext() {
			return globalMetaAttributeContext;
		}


		@Override
		public ServiceRegistry getServiceRegistry() {
			return options.getServiceRegistry();
		}

		@Override
		public boolean quoteIdentifiersInContext() {
			return options.getDatabaseDefaults().isGloballyQuotedIdentifiers();
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// BindingContext deprecated impls

		@Override
		public <T> Class<T> locateClassByName(String name) {
			return classLoaderService.classForName( name );
		}

		@Override
		public Type makeDomainType(String className) {
			return new BasicType( className, makeJavaClassReference( className ) );
		}

		private Map<String, JavaClassReference> nameToJavaTypeMap = new HashMap<String, JavaClassReference>();

		@Override
		public JavaClassReference makeJavaClassReference(String className) {
			if ( className == null ) {
				throw new IllegalArgumentException( "className must be non-null." );
			}
			JavaClassReference javaClassReference = nameToJavaTypeMap.get( className );
			if ( javaClassReference == null ) {
				javaClassReference = new JavaClassReference( className, classLoaderService );
				nameToJavaTypeMap.put( className, javaClassReference );
			}
			return javaClassReference;
		}

		@Override
		public JavaClassReference makeJavaClassReference(Class<?> clazz) {
			if ( clazz == null ) {
				throw new IllegalArgumentException( "clazz must be non-null." );
			}
			JavaClassReference javaClassReference = nameToJavaTypeMap.get( clazz.getName() );
			if ( javaClassReference == null ) {
				javaClassReference = new JavaClassReference( clazz );
				nameToJavaTypeMap.put( clazz.getName(), javaClassReference );
			}
			return javaClassReference;
		}

		@Override
		public JavaClassReference makeJavaPropertyClassReference(
				JavaClassReference propertyContainerClassReference, String propertyName) {
			if ( propertyContainerClassReference == null || propertyName == null ) {
				throw new IllegalArgumentException( "propertyContainerClassReference and propertyName must be non-null." );
			}
			try {
				return makeJavaClassReference(
						ReflectHelper.reflectedPropertyClass(
								propertyContainerClassReference.getResolvedClass(),
								propertyName
						)
				);
			}
			catch ( Exception ignore ) {
				log.debugf(
						"Unable to locate attribute [%s] on class [%s]",
						propertyName,
						propertyContainerClassReference.getName()
				);
			}
			return null;
		}
	}

	public static class MappingDefaultsImpl implements MappingDefaults {
		private static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
		private static final String DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME = "tenant_id";
		private static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";
		private static final String DEFAULT_CASCADE = "none";
		private static final String DEFAULT_PROPERTY_ACCESS = "property";

		private final MetadataBuildingOptions options;

		public MappingDefaultsImpl(MetadataBuildingOptions options) {
			this.options = options;
		}

		@Override
		public String getPackageName() {
			// default package name is only relevant within processing XML mappings.  Here
			// at the root, there is no default
			return null;
		}

		@Override
		public String getSchemaName() {
			return options.getDatabaseDefaults().getDefaultSchemaName();
		}

		@Override
		public String getCatalogName() {
			return options.getDatabaseDefaults().getDefaultCatalogName();
		}

		@Override
		public String getIdColumnName() {
			return DEFAULT_IDENTIFIER_COLUMN_NAME;
		}

		@Override
		public String getTenantIdColumnName() {
			return DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME;
		}

		@Override
		public String getDiscriminatorColumnName() {
			return DEFAULT_DISCRIMINATOR_COLUMN_NAME;
		}

		@Override
		public String getCascadeStyle() {
			return DEFAULT_CASCADE;
		}

		@Override
		public String getPropertyAccessorName() {
			return DEFAULT_PROPERTY_ACCESS;
		}

		@Override
		public boolean areAssociationsLazy() {
			return true;
		}

		@Override
		public AccessType getCacheAccessType() {
			return options.getDefaultCacheAccessType();
		}
	}

	public static class InFlightMetadataCollectorImpl implements InFlightMetadataCollector {
		private final MetadataBuildingOptions options;
		private final TypeResolver typeResolver;

		private final Database database;
		private final ObjectNameNormalizer nameNormalizer;
		private final MutableIdentifierGeneratorFactory identifierGeneratorFactory;

		private final Map<String, TypeDefinition> typeDefinitionMap = new HashMap<String, TypeDefinition>();
		private final Map<String, FilterDefinition> filterDefinitionMap = new HashMap<String, FilterDefinition>();

		private final Map<String, EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();
		private final Map<String, PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();
		private final Map<String, FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();
		private final Map<String, String> imports = new HashMap<String, String>();
		private final Map<String, IdentifierGeneratorDefinition> idGenerators = new HashMap<String, IdentifierGeneratorDefinition>();
		private final Map<String, NamedQueryDefinition> namedQueryDefs = new HashMap<String, NamedQueryDefinition>();
		private final Map<String, NamedSQLQueryDefinition> namedNativeQueryDefs = new HashMap<String, NamedSQLQueryDefinition>();
		private final Map<String, ResultSetMappingDefinition> resultSetMappings = new HashMap<String, ResultSetMappingDefinition>();
		private final Map<String, NamedEntityGraphDefinition> namedEntityGraphMap = new HashMap<String, NamedEntityGraphDefinition>(  );
		private final Map<Identifier, SecondaryTable> secondaryTableMap = new HashMap<Identifier, SecondaryTable>();

		public InFlightMetadataCollectorImpl(MetadataBuildingOptions options, TypeResolver typeResolver) {
			this.options = options;
			this.typeResolver = typeResolver;

			this.database = new Database(
					options.getDatabaseDefaults(),
					options.getServiceRegistry().getService( JdbcServices.class ).getJdbcEnvironment()
			);

			this.nameNormalizer = new ObjectNameNormalizer() {
				@Override
				protected NamingStrategy getNamingStrategy() {
					return InFlightMetadataCollectorImpl.this.options.getNamingStrategy();
				}

				@Override
				protected boolean isUseQuotedIdentifiersGlobally() {
					return InFlightMetadataCollectorImpl.this.options.getDatabaseDefaults().isGloballyQuotedIdentifiers();
				}
			};

			this.identifierGeneratorFactory = options.getServiceRegistry().getService( MutableIdentifierGeneratorFactory.class );
		}

		@Override
		public Database getDatabase() {
			return database;
		}

		@Override
		public TypeResolver getTypeResolver() {
			return typeResolver;
		}

		@Override
		public ObjectNameNormalizer getObjectNameNormalizer() {
			return nameNormalizer;
		}

		@Override
		public void setGloballyQuotedIdentifiers(boolean b) {
			// hmmm...
		}

		@Override
		public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
			return identifierGeneratorFactory;
		}

		@Override
		public SessionFactoryBuilder getSessionFactoryBuilder() {
			throw new UnsupportedOperationException(
					"You should not be building a SessionFactory from an in-flight metadata collector; and of course " +
							"we should better segment this in the API :)"
			);
		}

		@Override
		public SessionFactory buildSessionFactory() {
			throw new UnsupportedOperationException(
					"You should not be building a SessionFactory from an in-flight metadata collector; and of course " +
							"we should better segment this in the API :)"
			);
		}



		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Hibernate Type handling

		@Override
		public void addTypeDefinition( TypeDefinition typeDefinition ) {
			if ( typeDefinition == null ) {
				throw new IllegalArgumentException( "Type definition is null" );
			}

			// Need to register both by name and registration keys.
			if ( !StringHelper.isEmpty( typeDefinition.getName() ) ) {
				addTypeDefinition( typeDefinition.getName(), typeDefinition );
			}

			for ( String registrationKey : typeDefinition.getRegistrationKeys() ) {
				addTypeDefinition( registrationKey, typeDefinition );
			}
		}

		private void addTypeDefinition( String registrationKey, TypeDefinition typeDefinition ) {
			final TypeDefinition previous = typeDefinitionMap.put(
					registrationKey, typeDefinition );
			if ( previous != null ) {
				log.debugf(
						"Duplicate typedef name [%s] now -> %s",
						registrationKey,
						typeDefinition.getTypeImplementorClass().getName()
				);
			}
		}

		@Override
		public Iterable<TypeDefinition> getTypeDefinitions() {
			return typeDefinitionMap.values();
		}

		@Override
		public boolean hasTypeDefinition(String registrationKey) {
			return typeDefinitionMap.containsKey( registrationKey );
		}

		@Override
		public TypeDefinition getTypeDefinition(String registrationKey) {
			return typeDefinitionMap.get( registrationKey );
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// filter definitions

		@Override
		public void addFilterDefinition(FilterDefinition filterDefinition) {
			if ( filterDefinition == null || filterDefinition.getFilterName() == null ) {
				throw new IllegalArgumentException( "Filter definition object or name is null: "  + filterDefinition );
			}
			filterDefinitionMap.put( filterDefinition.getFilterName(), filterDefinition );
		}
		@Override
		public Map<String, FilterDefinition> getFilterDefinitions() {
			return filterDefinitionMap;
		}



		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// fetch profiles

		@Override
		public void addFetchProfile(FetchProfile profile) {
			if ( profile == null || profile.getName() == null ) {
				throw new IllegalArgumentException( "Fetch profile object or name is null: " + profile );
			}
			FetchProfile old = fetchProfiles.put( profile.getName(), profile );
			if ( old != null ) {
				log.warn( "Duplicated fetch profile with same name [" + profile.getName() + "] found." );
			}
		}

		@Override
		public Iterable<FetchProfile> getFetchProfiles() {
			return fetchProfiles.values();
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// identifier generators

		@Override
		public void addIdGenerator(IdentifierGeneratorDefinition generator) {
			if ( generator == null || generator.getName() == null ) {
				throw new IllegalArgumentException( "ID generator object or name is null." );
			}
			idGenerators.put( generator.getName(), generator );
		}

		@Override
		public IdentifierGeneratorDefinition getIdGenerator(String name) {
			if ( name == null ) {
				throw new IllegalArgumentException( "null is not a valid generator name" );
			}
			return idGenerators.get( name );
		}

		@Override
		public void registerIdentifierGenerator(String name, String generatorClassName) {
			identifierGeneratorFactory.register(
					name,
					options.getServiceRegistry()
							.getService( ClassLoaderService.class )
							.classForName( generatorClassName )
			);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Named EntityGraph handling

		@Override
		public void addNamedEntityGraph(NamedEntityGraphDefinition definition) {
			final String name = definition.getRegisteredName();
			final NamedEntityGraphDefinition previous = namedEntityGraphMap.put( name, definition );
			if ( previous != null ) {
				throw new DuplicateMappingException( "NamedEntityGraph", name );
			}
		}

		@Override
		public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs() {
			return namedEntityGraphMap;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Named query handling

		@Override
		public void addNamedNativeQuery(NamedSQLQueryDefinition def) {
			if ( def == null ) {
				throw new IllegalArgumentException( "Named native query definition object is null" );
			}
			if ( def.getName() == null ) {
				throw new IllegalArgumentException( "Named native query definition name is null: " + def.getQueryString() );
			}
			NamedSQLQueryDefinition old = namedNativeQueryDefs.put( def.getName(), def );
			if ( old != null ) {
				log.warn( "Duplicated named query with same name["+ old.getName() +"] found" );
				//todo mapping exception??
				// in the old metamodel, the NamedQueryDefinition.name actually not is not
				// always the one defined in the hbm.  there are two cases:
				// 		1) if this <query> or <sql-query> is a sub-element of <hibernate-mapping> then,
				// 			then name is as it is
				//		2) if defined inside a class mapping, then the query name is actually
				// 			prefixed with the entity name (entityName.query_name), and the referenced sql
				// 			resultset mapping's name should also in this form.
			}
		}

		@Override
		public Iterable<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions() {
			return namedNativeQueryDefs.values();
		}

		@Override
		public void addNamedQuery(NamedQueryDefinition def) {
			if ( def == null ) {
				throw new IllegalArgumentException( "Named query definition is null" );
			}
			else if ( def.getName() == null ) {
				throw new IllegalArgumentException( "Named query definition name is null: " + def.getQueryString() );
			}
			NamedQueryDefinition old = namedQueryDefs.put( def.getName(), def );
			if ( old != null ) {
				log.warn( "Duplicated named query with same name["+ old.getName() +"] found" );
				//todo mapping exception??
				// see lengthy comment above
			}
		}

		public NamedQueryDefinition getNamedQuery(String name) {
			if ( name == null ) {
				throw new IllegalArgumentException( "null is not a valid query name" );
			}
			return namedQueryDefs.get( name );
		}

		@Override
		public Iterable<NamedQueryDefinition> getNamedQueryDefinitions() {
			return namedQueryDefs.values();
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// result-set mapping handling

		@Override
		public void addResultSetMapping(ResultSetMappingDefinition resultSetMappingDefinition) {
			if ( resultSetMappingDefinition == null || resultSetMappingDefinition.getName() == null ) {
				throw new IllegalArgumentException( "Result-set mapping object or name is null: " + resultSetMappingDefinition );
			}
			ResultSetMappingDefinition old = resultSetMappings.put(
					resultSetMappingDefinition.getName(),
					resultSetMappingDefinition
			);
			if ( old != null ) {
				log.warn( "Duplicated sql result set mapping with same name["+ resultSetMappingDefinition.getName() +"] found" );
				//todo mapping exception??
			}
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// imports

		@Override
		public Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions() {
			return resultSetMappings;
		}

		@Override
		public void addImport(String importName, String entityName) {
			if ( importName == null || entityName == null ) {
				throw new IllegalArgumentException( "Import name or entity name is null" );
			}
			log.tracev( "Import: {0} -> {1}", importName, entityName );
			String old = imports.put( importName, entityName );
			if ( old != null ) {
				log.debug( "import name [" + importName + "] overrode previous [{" + old + "}]" );
			}
		}

		@Override
		public Map<String,String> getImports() {
			return imports;
		}

		@Override
		public NamedSQLQueryDefinition getNamedNativeQuery(String name) {
			return namedNativeQueryDefs.get( name );
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// entity bindings

		public EntityBinding getEntityBinding(String entityName) {
			return entityBindingMap.get( entityName );
		}

		@Override
		public EntityBinding getRootEntityBinding(String entityName) {
			EntityBinding binding = entityBindingMap.get( entityName );
			if ( binding == null ) {
				throw new IllegalStateException( "Unknown entity binding: " + entityName );
			}

			do {
				if ( binding.isRoot() ) {
					return binding;
				}
				binding = binding.getSuperEntityBinding();
			} while ( binding != null );

			throw new AssertionFailure( "Entity binding has no root: " + entityName );
		}

		@Override
		public Iterable<EntityBinding> getEntityBindings() {
			return entityBindingMap.values();
		}

		@Override
		public void addEntity(EntityBinding entityBinding) {
			final String entityName = entityBinding.getEntity().getName();
			if ( entityBindingMap.containsKey( entityName ) ) {
				throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, entityName );
			}
			entityBindingMap.put( entityName, entityBinding );
			final boolean isPOJO = entityBinding.getHierarchyDetails().getEntityMode() == EntityMode.POJO;
			final String className = isPOJO ? entityBinding.getEntity().getClassReference().getName() : null;
			if ( isPOJO && StringHelper.isEmpty( className ) ) {
				throw new MappingException( "Entity[" + entityName + "] is mapped as pojo but don't have a class name" );
			}
			if ( StringHelper.isNotEmpty( className ) && !entityBindingMap.containsKey( className ) ) {
				entityBindingMap.put( className, entityBinding );
			}
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// secondary table handling

		@Override
		public void addSecondaryTable(SecondaryTable secondaryTable) {
			secondaryTableMap.put( secondaryTable.getSecondaryTableReference().getLogicalName(), secondaryTable );
		}

		@Override
		public Map<Identifier, SecondaryTable> getSecondaryTables() {
			return secondaryTableMap;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// collection binding handling

		public PluralAttributeBinding getCollection(String collectionRole) {
			return collectionBindingMap.get( collectionRole );
		}

		@Override
		public Iterable<PluralAttributeBinding> getCollectionBindings() {
			return collectionBindingMap.values();
		}

		@Override
		public void addCollection(PluralAttributeBinding pluralAttributeBinding) {
			final String owningEntityName = pluralAttributeBinding.getContainer().seekEntityBinding().getEntityName();
			final String containerPathBase = pluralAttributeBinding.getContainer().getPathBase();
			final String attributeName = pluralAttributeBinding.getAttribute().getName();
			final String collectionRole;
			if ( StringHelper.isEmpty( containerPathBase ) ) {
				collectionRole = owningEntityName + '.' + attributeName;
			}
			else {
				collectionRole = owningEntityName + '.' + containerPathBase + '.' + attributeName;
			}
			if ( collectionBindingMap.containsKey( collectionRole ) ) {
				throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, collectionRole );
			}
			collectionBindingMap.put( collectionRole, pluralAttributeBinding );
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Mapping impl

		@Override
		public org.hibernate.type.Type getIdentifierType(String entityName) throws MappingException {
			EntityBinding entityBinding = getEntityBinding( entityName );
			if ( entityBinding == null ) {
				throw new MappingException( "Entity binding not known: " + entityName );
			}
			return entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding()
					.getHibernateTypeDescriptor()
					.getResolvedTypeMapping();
		}

		@Override
		public String getIdentifierPropertyName(String entityName) throws MappingException {
			EntityBinding entityBinding = getEntityBinding( entityName );
			if ( entityBinding == null ) {
				throw new MappingException( "Entity binding not known: " + entityName );
			}
			AttributeBinding idBinding = entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
			return idBinding == null ? null : idBinding.getAttribute().getName();
		}

		@Override
		public org.hibernate.type.Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
			EntityBinding entityBinding = getEntityBinding( entityName );
			if ( entityBinding == null ) {
				throw new MappingException( "Entity binding not known: " + entityName );
			}
			AttributeBinding attributeBinding = entityBinding.locateAttributeBindingByPath( propertyName, true );
			if ( attributeBinding == null ) {
				throw new MappingException( "unknown property: " + entityName + '.' + propertyName );
			}
			return attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
		}

		public MetadataImpl buildMetadataInstance() {
			return new MetadataImpl(
					options.getServiceRegistry(),
					database,
					typeResolver,
					identifierGeneratorFactory,
					typeDefinitionMap,
					filterDefinitionMap,
					entityBindingMap,
					collectionBindingMap,
					fetchProfiles,
					imports,
					idGenerators,
					namedQueryDefs,
					namedNativeQueryDefs,
					resultSetMappings,
					namedEntityGraphMap,
					secondaryTableMap
			);
		}
	}
}
