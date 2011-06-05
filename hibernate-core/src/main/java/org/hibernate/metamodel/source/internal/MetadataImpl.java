/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.DuplicateMappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.id.factory.DefaultIdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SourceProcessingOrder;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.metamodel.source.annotations.AnnotationBinder;
import org.hibernate.metamodel.source.hbm.HbmBinder;
import org.hibernate.metamodel.source.spi.Binder;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.type.TypeResolver;

/**
 * Container for configuration data collected during binding the metamodel.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class MetadataImpl implements MetadataImplementor, Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MetadataImpl.class.getName()
	);

	private final BasicServiceRegistry serviceRegistry;
	private final Options options;
	private ClassLoaderService classLoaderService;

	private TypeResolver typeResolver = new TypeResolver();
	private DefaultIdentifierGeneratorFactory identifierGeneratorFactory = new DefaultIdentifierGeneratorFactory();

	private final Database database = new Database();

	private final MappingDefaults mappingDefaults;

	/**
	 * Maps the fully qualified class name of an entity to its entity binding
	 */
	private Map<String, EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();
	private Map<String, PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();
	private Map<String, FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();
	private Map<String, String> imports;
	private Map<String, TypeDef> typeDefs = new HashMap<String, TypeDef>();
	private Map<String, IdGenerator> idGenerators = new HashMap<String, IdGenerator>();
	private Map<String, NamedQueryDefinition> namedQueryDefs = new HashMap<String, NamedQueryDefinition>();
	private Map<String, NamedSQLQueryDefinition> namedNativeQueryDefs = new HashMap<String, NamedSQLQueryDefinition>();
	private Map<String, FilterDefinition> filterDefs = new HashMap<String, FilterDefinition>();

	// todo : keep as part of Database?
	private List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects = new ArrayList<AuxiliaryDatabaseObject>();

	public MetadataImpl(MetadataSources metadataSources, Options options) {
		this.serviceRegistry = metadataSources.getServiceRegistry();
		this.options = options;

		this.mappingDefaults = new MappingDefaultsImpl();

		final Binder[] binders;
		if ( options.getSourceProcessingOrder() == SourceProcessingOrder.HBM_FIRST ) {
			binders = new Binder[] {
					new HbmBinder( this ),
					new AnnotationBinder( this )
			};
		}
		else {
			binders = new Binder[] {
					new AnnotationBinder( this ),
					new HbmBinder( this )
			};
		}

		final ArrayList<String> processedEntityNames = new ArrayList<String>();

		prepare( binders, metadataSources );
		bindIndependentMetadata( binders, metadataSources );
		bindTypeDependentMetadata( binders, metadataSources );
		bindMappingMetadata( binders, metadataSources, processedEntityNames );
		bindMappingDependentMetadata( binders, metadataSources );

		// todo : remove this by coordinated ordering of entity processing
		new EntityReferenceResolver( this ).resolve();
	}

	private void prepare(Binder[] binders, MetadataSources metadataSources) {
		for ( Binder binder : binders ) {
			binder.prepare( metadataSources );
		}
	}

	private void bindIndependentMetadata(Binder[] binders, MetadataSources metadataSources) {
		for ( Binder binder : binders ) {
			binder.bindIndependentMetadata( metadataSources );
		}
	}

	private void bindTypeDependentMetadata(Binder[] binders, MetadataSources metadataSources) {
		for ( Binder binder : binders ) {
			binder.bindTypeDependentMetadata( metadataSources );
		}
	}

	private void bindMappingMetadata(Binder[] binders, MetadataSources metadataSources, List<String> processedEntityNames) {
		for ( Binder binder : binders ) {
			binder.bindMappingMetadata( metadataSources, processedEntityNames );
		}
	}

	private void bindMappingDependentMetadata(Binder[] binders, MetadataSources metadataSources) {
		for ( Binder binder : binders ) {
			binder.bindMappingDependentMetadata( metadataSources );
		}
	}

	@Override
	public void addFetchProfile(FetchProfile profile) {
		fetchProfiles.put( profile.getName(), profile );
	}

	@Override
	public void addFilterDefinition(FilterDefinition def) {
		filterDefs.put( def.getFilterName(), def );
	}

	public Iterable<FilterDefinition> getFilterDefinitions() {
		return filterDefs.values();
	}

	@Override
	public void addIdGenerator(IdGenerator generator) {
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
	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		auxiliaryDatabaseObjects.add( auxiliaryDatabaseObject );
	}

	@Override
	public void addNamedNativeQuery(String name, NamedSQLQueryDefinition def) {
		namedNativeQueryDefs.put( name, def );
	}

	public NamedSQLQueryDefinition getNamedNativeQuery(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid native query name" );
		}
		return namedNativeQueryDefs.get( name );
	}

	@Override
	public void addNamedQuery(String name, NamedQueryDefinition def) {
		namedQueryDefs.put( name, def );
	}

	public NamedQueryDefinition getNamedQuery(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid query name" );
		}
		return namedQueryDefs.get( name );
	}

	@Override
	public void addTypeDefinition(TypeDef typeDef) {
		final TypeDef previous = typeDefs.put( typeDef.getName(), typeDef );
		if ( previous != null ) {
			LOG.debugf( "Duplicate typedef name [%s] now -> %s", typeDef.getName(), typeDef.getTypeClass() );
		}
	}

	@Override
	public Iterable<TypeDef> getTypeDefinitions() {
		return typeDefs.values();
	}

	public TypeDef getTypeDef(String name) {
		return typeDefs.get( name );
	}

	private ClassLoaderService classLoaderService(){
		if(classLoaderService==null){
			classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		}
		return classLoaderService;
	}

	@Override
	public Options getOptions() {
		return options;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		// todo : implement!!!!
		return null;
	}

	@Override
	public BasicServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	public EntityBinding getEntityBinding(String entityName) {
		return entityBindingMap.get( entityName );
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

	public Iterable<PluralAttributeBinding> getCollections() {
		return collectionBindingMap.values();
	}

	public void addCollection(PluralAttributeBinding pluralAttributeBinding) {
		final String owningEntityName = pluralAttributeBinding.getEntityBinding().getEntity().getName();
		final String attributeName = pluralAttributeBinding.getAttribute().getName();
		final String collectionRole = owningEntityName + '.' + attributeName;
		if ( collectionBindingMap.containsKey( collectionRole ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, collectionRole );
		}
		collectionBindingMap.put( collectionRole, pluralAttributeBinding );
	}

	public void addImport(String importName, String entityName) {
		if ( imports == null ) {
			imports = new HashMap<String, String>();
		}
		LOG.trace( "Import: " + importName + " -> " + entityName );
		String old = imports.put( importName, entityName );
		if ( old != null ) {
			LOG.debug( "import name [" + importName + "] overrode previous [{" + old + "}]" );
		}
	}

	public Iterable<FetchProfile> getFetchProfiles() {
		return fetchProfiles.values();
	}

	public TypeResolver getTypeResolver() {
		return typeResolver;
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return options.getNamingStrategy();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	private final MetaAttributeContext globalMetaAttributeContext = new MetaAttributeContext();

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
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

	private class MappingDefaultsImpl implements MappingDefaults {

		@Override
		public String getPackageName() {
			return null;
		}

		@Override
		public String getDefaultSchemaName() {
			return options.getDefaultSchemaName();
		}

		@Override
		public String getDefaultCatalogName() {
			return options.getDefaultCatalogName();
		}

		@Override
		public String getDefaultIdColumnName() {
			return DEFAULT_IDENTIFIER_COLUMN_NAME;
		}

		@Override
		public String getDefaultDiscriminatorColumnName() {
			return DEFAULT_DISCRIMINATOR_COLUMN_NAME;
		}

		@Override
		public String getDefaultCascade() {
			return DEFAULT_CASCADE;
		}

		@Override
		public String getDefaultAccess() {
			return DEFAULT_PROPERTY_ACCESS;
		}

		@Override
		public boolean isDefaultLazy() {
			return true;
		}

		@Override
		public Map<String, MetaAttribute> getMappingMetas() {
			return Collections.emptyMap();
		}
	}
}
