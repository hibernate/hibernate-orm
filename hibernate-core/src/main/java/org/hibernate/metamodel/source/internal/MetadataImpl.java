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
package org.hibernate.metamodel.source.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.DuplicateMappingException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.MetadataSourceProcessingOrder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.BasicType;
import org.hibernate.metamodel.domain.Type;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.metamodel.source.MappingDefaults;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.MetadataSourceProcessor;
import org.hibernate.metamodel.source.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.metamodel.source.hbm.HbmMetadataSourceProcessorImpl;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.type.TypeResolver;

/**
 * Container for configuration data collected during binding the metamodel.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class MetadataImpl implements MetadataImplementor, Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MetadataImpl.class.getName()
	);

	private final ServiceRegistry serviceRegistry;
	private final Options options;

	private final ValueHolder<ClassLoaderService> classLoaderService;
	private final ValueHolder<PersisterClassResolver> persisterClassResolverService;

	private TypeResolver typeResolver = new TypeResolver();

	private SessionFactoryBuilder sessionFactoryBuilder = new SessionFactoryBuilderImpl( this );

	private final MutableIdentifierGeneratorFactory identifierGeneratorFactory;

	private final Database database;

	private final MappingDefaults mappingDefaults;

	/**
	 * Maps the fully qualified class name of an entity to its entity binding
	 */
	private Map<String, EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();

	private Map<String, PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();
	private Map<String, FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();
	private Map<String, String> imports = new HashMap<String, String>();
	private Map<String, TypeDef> typeDefs = new HashMap<String, TypeDef>();
	private Map<String, IdGenerator> idGenerators = new HashMap<String, IdGenerator>();
	private Map<String, NamedQueryDefinition> namedQueryDefs = new HashMap<String, NamedQueryDefinition>();
	private Map<String, NamedSQLQueryDefinition> namedNativeQueryDefs = new HashMap<String, NamedSQLQueryDefinition>();
	private Map<String, ResultSetMappingDefinition> resultSetMappings = new HashMap<String, ResultSetMappingDefinition>();
	private Map<String, FilterDefinition> filterDefs = new HashMap<String, FilterDefinition>();

    private boolean globallyQuotedIdentifiers = false;

	public MetadataImpl(MetadataSources metadataSources, Options options) {
		this.serviceRegistry =  metadataSources.getServiceRegistry();
		this.options = options;
		this.identifierGeneratorFactory = serviceRegistry.getService( MutableIdentifierGeneratorFactory.class );
				//new DefaultIdentifierGeneratorFactory( dialect );
		this.database = new Database( options );

		this.mappingDefaults = new MappingDefaultsImpl();

		final MetadataSourceProcessor[] metadataSourceProcessors;
		if ( options.getMetadataSourceProcessingOrder() == MetadataSourceProcessingOrder.HBM_FIRST ) {
			metadataSourceProcessors = new MetadataSourceProcessor[] {
					new HbmMetadataSourceProcessorImpl( this ),
					new AnnotationMetadataSourceProcessorImpl( this )
			};
		}
		else {
			metadataSourceProcessors = new MetadataSourceProcessor[] {
					new AnnotationMetadataSourceProcessorImpl( this ),
					new HbmMetadataSourceProcessorImpl( this )
			};
		}

		this.classLoaderService = new ValueHolder<ClassLoaderService>(
				new ValueHolder.DeferredInitializer<ClassLoaderService>() {
					@Override
					public ClassLoaderService initialize() {
						return serviceRegistry.getService( ClassLoaderService.class );
					}
				}
		);
		this.persisterClassResolverService = new ValueHolder<PersisterClassResolver>(
				new ValueHolder.DeferredInitializer<PersisterClassResolver>() {
					@Override
					public PersisterClassResolver initialize() {
						return serviceRegistry.getService( PersisterClassResolver.class );
					}
				}
		);


		final ArrayList<String> processedEntityNames = new ArrayList<String>();

		prepare( metadataSourceProcessors, metadataSources );
		bindIndependentMetadata( metadataSourceProcessors, metadataSources );
		bindTypeDependentMetadata( metadataSourceProcessors, metadataSources );
		bindMappingMetadata( metadataSourceProcessors, metadataSources, processedEntityNames );
		bindMappingDependentMetadata( metadataSourceProcessors, metadataSources );

		// todo : remove this by coordinated ordering of entity processing
		new AssociationResolver( this ).resolve();
		new HibernateTypeResolver( this ).resolve();
		// IdentifierGeneratorResolver.resolve() must execute after AttributeTypeResolver.resolve()
		new IdentifierGeneratorResolver( this ).resolve();
	}

	private void prepare(MetadataSourceProcessor[] metadataSourceProcessors, MetadataSources metadataSources) {
		for ( MetadataSourceProcessor metadataSourceProcessor : metadataSourceProcessors ) {
			metadataSourceProcessor.prepare( metadataSources );
		}
	}

	private void bindIndependentMetadata(MetadataSourceProcessor[] metadataSourceProcessors, MetadataSources metadataSources) {
		for ( MetadataSourceProcessor metadataSourceProcessor : metadataSourceProcessors ) {
			metadataSourceProcessor.processIndependentMetadata( metadataSources );
		}
	}

	private void bindTypeDependentMetadata(MetadataSourceProcessor[] metadataSourceProcessors, MetadataSources metadataSources) {
		for ( MetadataSourceProcessor metadataSourceProcessor : metadataSourceProcessors ) {
			metadataSourceProcessor.processTypeDependentMetadata( metadataSources );
		}
	}

	private void bindMappingMetadata(MetadataSourceProcessor[] metadataSourceProcessors, MetadataSources metadataSources, List<String> processedEntityNames) {
		for ( MetadataSourceProcessor metadataSourceProcessor : metadataSourceProcessors ) {
			metadataSourceProcessor.processMappingMetadata( metadataSources, processedEntityNames );
		}
	}

	private void bindMappingDependentMetadata(MetadataSourceProcessor[] metadataSourceProcessors, MetadataSources metadataSources) {
		for ( MetadataSourceProcessor metadataSourceProcessor : metadataSourceProcessors ) {
			metadataSourceProcessor.processMappingDependentMetadata( metadataSources );
		}
	}

	@Override
	public void addFetchProfile(FetchProfile profile) {
		if ( profile == null || profile.getName() == null ) {
			throw new IllegalArgumentException( "Fetch profile object or name is null: " + profile );
		}
		fetchProfiles.put( profile.getName(), profile );
	}

	@Override
	public void addFilterDefinition(FilterDefinition def) {
		if ( def == null || def.getFilterName() == null ) {
			throw new IllegalArgumentException( "Filter definition object or name is null: "  + def );
		}
		filterDefs.put( def.getFilterName(), def );
	}

	public Iterable<FilterDefinition> getFilterDefinitions() {
		return filterDefs.values();
	}

	@Override
	public void addIdGenerator(IdGenerator generator) {
		if ( generator == null || generator.getName() == null ) {
			throw new IllegalArgumentException( "ID generator object or name is null." );
		}
		idGenerators.put( generator.getName(), generator );
	}

	@Override
	public IdGenerator getIdGenerator(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid generator name" );
		}
		return idGenerators.get( name );
	}
	@Override
	public void registerIdentifierGenerator(String name, String generatorClassName) {
		 identifierGeneratorFactory.register( name, classLoaderService().classForName( generatorClassName ) );
	}

	@Override
	public void addNamedNativeQuery(NamedSQLQueryDefinition def) {
		if ( def == null || def.getName() == null ) {
			throw new IllegalArgumentException( "Named native query definition object or name is null: " + def.getQueryString() );
		}
		namedNativeQueryDefs.put( def.getName(), def );
	}

	public NamedSQLQueryDefinition getNamedNativeQuery(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid native query name" );
		}
		return namedNativeQueryDefs.get( name );
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
		namedQueryDefs.put( def.getName(), def );
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

	@Override
	public void addResultSetMapping(ResultSetMappingDefinition resultSetMappingDefinition) {
		if ( resultSetMappingDefinition == null || resultSetMappingDefinition.getName() == null ) {
			throw new IllegalArgumentException( "Result-set mapping object or name is null: " + resultSetMappingDefinition );
		}
		resultSetMappings.put( resultSetMappingDefinition.getName(), resultSetMappingDefinition );
	}

	@Override
	public Iterable<ResultSetMappingDefinition> getResultSetMappingDefinitions() {
		return resultSetMappings.values();
	}

	@Override
	public void addTypeDefinition(TypeDef typeDef) {
		if ( typeDef == null ) {
			throw new IllegalArgumentException( "Type definition is null" );
		}
		else if ( typeDef.getName() == null ) {
			throw new IllegalArgumentException( "Type definition name is null: " + typeDef.getTypeClass() );
		}
		final TypeDef previous = typeDefs.put( typeDef.getName(), typeDef );
		if ( previous != null ) {
			LOG.debugf( "Duplicate typedef name [%s] now -> %s", typeDef.getName(), typeDef.getTypeClass() );
		}
	}

	@Override
	public Iterable<TypeDef> getTypeDefinitions() {
		return typeDefs.values();
	}

	@Override
	public TypeDef getTypeDefinition(String name) {
		return typeDefs.get( name );
	}

	private ClassLoaderService classLoaderService() {
		return classLoaderService.getValue();
	}

	private PersisterClassResolver persisterClassResolverService() {
		return persisterClassResolverService.getValue();
	}

	@Override
	public Options getOptions() {
		return options;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return sessionFactoryBuilder.buildSessionFactory();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> Class<T> locateClassByName(String name) {
		return classLoaderService().classForName( name );
	}

	@Override
	public Type makeJavaType(String className) {
		// todo : have this perform some analysis of the incoming type name to determine appropriate return
		return new BasicType( className, makeClassReference( className ) );
	}

	@Override
	public ValueHolder<Class<?>> makeClassReference(final String className) {
		return new ValueHolder<Class<?>>(
				new ValueHolder.DeferredInitializer<Class<?>>() {
					@Override
					public Class<?> initialize() {
						return classLoaderService.getValue().classForName( className );
					}
				}
		);
	}

	@Override
	public String qualifyClassName(String name) {
		return name;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

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

	public Iterable<EntityBinding> getEntityBindings() {
		return entityBindingMap.values();
	}

	public void addEntity(EntityBinding entityBinding) {
		final String entityName = entityBinding.getEntity().getName();
		if ( entityBindingMap.containsKey( entityName ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, entityName );
		}
		entityBindingMap.put( entityName, entityBinding );
	}

	public PluralAttributeBinding getCollection(String collectionRole) {
		return collectionBindingMap.get( collectionRole );
	}

	@Override
	public Iterable<PluralAttributeBinding> getCollectionBindings() {
		return collectionBindingMap.values();
	}

	public void addCollection(PluralAttributeBinding pluralAttributeBinding) {
		final String owningEntityName = pluralAttributeBinding.getContainer().getPathBase();
		final String attributeName = pluralAttributeBinding.getAttribute().getName();
		final String collectionRole = owningEntityName + '.' + attributeName;
		if ( collectionBindingMap.containsKey( collectionRole ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, collectionRole );
		}
		collectionBindingMap.put( collectionRole, pluralAttributeBinding );
	}

	public void addImport(String importName, String entityName) {
		if ( importName == null || entityName == null ) {
			throw new IllegalArgumentException( "Import name or entity name is null" );
		}
		LOG.tracev( "Import: {0} -> {1}", importName, entityName );
		String old = imports.put( importName, entityName );
		if ( old != null ) {
			LOG.debug( "import name [" + importName + "] overrode previous [{" + old + "}]" );
		}
	}

	@Override
	public Iterable<Map.Entry<String, String>> getImports() {
		return imports.entrySet();
	}

	@Override
	public Iterable<FetchProfile> getFetchProfiles() {
		return fetchProfiles.values();
	}

	public TypeResolver getTypeResolver() {
		return typeResolver;
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		return sessionFactoryBuilder;
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return options.getNamingStrategy();
	}

    @Override
    public boolean isGloballyQuotedIdentifiers() {
        return globallyQuotedIdentifiers || getOptions().isGloballyQuotedIdentifiers();
    }

    public void setGloballyQuotedIdentifiers(boolean globallyQuotedIdentifiers){
       this.globallyQuotedIdentifiers = globallyQuotedIdentifiers;
    }

    @Override
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	private final MetaAttributeContext globalMetaAttributeContext = new MetaAttributeContext();

	@Override
	public MetaAttributeContext getGlobalMetaAttributeContext() {
		return globalMetaAttributeContext;
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return this;
	}

	private static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	private static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";
	private static final String DEFAULT_CASCADE = "none";
	private static final String DEFAULT_PROPERTY_ACCESS = "property";

	@Override
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return identifierGeneratorFactory;
	}

	@Override
	public org.hibernate.type.Type getIdentifierType(String entityName) throws MappingException {
		EntityBinding entityBinding = getEntityBinding( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		return entityBinding
				.getHierarchyDetails()
				.getEntityIdentifier()
				.getValueBinding()
				.getHibernateTypeDescriptor()
				.getResolvedTypeMapping();
	}

	@Override
	public String getIdentifierPropertyName(String entityName) throws MappingException {
		EntityBinding entityBinding = getEntityBinding( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		AttributeBinding idBinding = entityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding();
		return idBinding == null ? null : idBinding.getAttribute().getName();
	}

	@Override
	public org.hibernate.type.Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
		EntityBinding entityBinding = getEntityBinding( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		// TODO: should this call EntityBinding.getReferencedAttributeBindingString), which does not exist yet?
		AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( propertyName );
		if ( attributeBinding == null ) {
			throw new MappingException( "unknown property: " + entityName + '.' + propertyName );
		}
		return attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
	}

	private class MappingDefaultsImpl implements MappingDefaults {

		@Override
		public String getPackageName() {
			return null;
		}

		@Override
		public String getSchemaName() {
			return options.getDefaultSchemaName();
		}

		@Override
		public String getCatalogName() {
			return options.getDefaultCatalogName();
		}

		@Override
		public String getIdColumnName() {
			return DEFAULT_IDENTIFIER_COLUMN_NAME;
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

		private final ValueHolder<AccessType> regionFactorySpecifiedDefaultAccessType = new ValueHolder<AccessType>(
				new ValueHolder.DeferredInitializer<AccessType>() {
					@Override
					public AccessType initialize() {
						final RegionFactory regionFactory = getServiceRegistry().getService( RegionFactory.class );
						return regionFactory.getDefaultAccessType();
					}
				}
		);

		@Override
		public AccessType getCacheAccessType() {
			return options.getDefaultAccessType() != null
					? options.getDefaultAccessType()
					: regionFactorySpecifiedDefaultAccessType.getValue();
		}
	}
}
