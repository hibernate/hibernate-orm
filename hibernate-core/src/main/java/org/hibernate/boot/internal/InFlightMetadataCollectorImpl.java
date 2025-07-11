/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MapsId;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.DuplicateMappingException;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.convert.internal.AttributeConverterManager;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.internal.AggregateComponentSecondPass;
import org.hibernate.boot.model.internal.AnnotatedClassType;
import org.hibernate.boot.model.internal.CreateKeySecondPass;
import org.hibernate.boot.model.internal.FkSecondPass;
import org.hibernate.boot.model.internal.IdGeneratorResolver;
import org.hibernate.boot.model.internal.ImplicitToOneJoinTableSecondPass;
import org.hibernate.boot.model.internal.OptionalDeterminationSecondPass;
import org.hibernate.boot.model.internal.QuerySecondPass;
import org.hibernate.boot.model.internal.SecondaryTableFromAnnotationSecondPass;
import org.hibernate.boot.model.internal.SecondaryTableSecondPass;
import org.hibernate.boot.model.internal.SetBasicValueTypeSecondPass;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.source.internal.ImplicitColumnNamingSecondPass;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.xml.internal.PersistenceUnitMetadataImpl;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.NaturalIdUniqueKeyBinder;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl.fromExplicit;
import static org.hibernate.cfg.MappingSettings.DEFAULT_CATALOG;
import static org.hibernate.cfg.MappingSettings.DEFAULT_SCHEMA;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * The implementation of the {@linkplain InFlightMetadataCollector in-flight
 * metadata collector contract}.
 * <p>
 * The usage expectation is that this class is used until all Metadata info is
 * collected and then {@link #buildMetadataInstance} is called to generate
 * the complete (and immutable) Metadata object.
 *
 * @author Steve Ebersole
 */
public class InFlightMetadataCollectorImpl
		implements InFlightMetadataCollector, ConverterRegistry, GeneratorSettings {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( InFlightMetadataCollectorImpl.class );

	private final BootstrapContext bootstrapContext;
	private final MetadataBuildingOptions options;

	private final GlobalRegistrations globalRegistrations;
	private final PersistenceUnitMetadata persistenceUnitMetadata;

	private final AttributeConverterManager attributeConverterManager = new AttributeConverterManager();

	private final UUID uuid;

	private final Map<String,PersistentClass> entityBindingMap = new HashMap<>();
	private final List<Component> composites = new ArrayList<>();
	private final Map<Class<?>, Component> genericComponentsMap = new HashMap<>();
	private final Map<ClassDetails, List<ClassDetails>> embeddableSubtypes = new HashMap<>();
	private final Map<Class<?>, DiscriminatorType<?>> embeddableDiscriminatorTypesMap = new HashMap<>();
	private final Map<String,Collection> collectionBindingMap = new HashMap<>();

	private final Map<String, FilterDefinition> filterDefinitionMap = new HashMap<>();
	private final Map<String, String> imports = new HashMap<>();

	private final TypeDefinitionRegistry typeDefRegistry = new TypeDefinitionRegistryStandardImpl();

	private Database database;

	private final Map<String, NamedHqlQueryDefinition<?>> namedQueryMap = new HashMap<>();
	private final Map<String, NamedNativeQueryDefinition<?>> namedNativeQueryMap = new HashMap<>();
	private final Map<String, NamedProcedureCallDefinition> namedProcedureCallMap = new HashMap<>();
	private final Map<String, NamedResultSetMappingDescriptor> sqlResultSetMappingMap = new HashMap<>();

	private final Map<String, NamedEntityGraphDefinition> namedEntityGraphMap = new HashMap<>();
	private final Map<String, FetchProfile> fetchProfileMap = new HashMap<>();
	private final Map<String, IdentifierGeneratorDefinition> idGeneratorDefinitionMap = new HashMap<>();

	private final Map<String, SqmFunctionDescriptor> sqlFunctionMap;

	final ConfigurationService configurationService;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// All the annotation-processing-specific state :(
	private final Set<String> defaultIdentifierGeneratorNames = new HashSet<>();
	private final Set<String> defaultNamedQueryNames = new HashSet<>();
	private final Set<String> defaultNamedNativeQueryNames = new HashSet<>();
	private final Set<String> defaultSqlResultSetMappingNames = new HashSet<>();
	private final Set<String> defaultNamedProcedureNames = new HashSet<>();
	private Map<Class<?>, MappedSuperclass> mappedSuperClasses;
	private Map<ClassDetails, Map<String, PropertyData>> propertiesAnnotatedWithMapsId;
	private Map<ClassDetails, Map<String, PropertyData>> propertiesAnnotatedWithIdAndToOne;
	private Map<String, String> mappedByResolver;
	private Map<String, String> propertyRefResolver;
	private Set<DelayedPropertyReferenceHandler> delayedPropertyReferenceHandlers;
	private List<Function<MetadataBuildingContext, Boolean>> valueResolvers;

	public InFlightMetadataCollectorImpl(
			BootstrapContext bootstrapContext,
			MetadataBuildingOptions options) {
		this.bootstrapContext = bootstrapContext;
		this.options = options;

		uuid = UUID.randomUUID();

		globalRegistrations = new GlobalRegistrationsImpl( bootstrapContext.getModelsContext(), bootstrapContext );
		persistenceUnitMetadata = new PersistenceUnitMetadataImpl();

		// we need this to be a ConcurrentHashMap for the one we ultimately pass along to the SF,
		// but is this the reference that gets passed along?
		sqlFunctionMap = new ConcurrentHashMap<>( bootstrapContext.getSqlFunctions() );

		bootstrapContext.getAuxiliaryDatabaseObjectList()
				.forEach( getDatabase()::addAuxiliaryDatabaseObject );

		configurationService = bootstrapContext.getConfigurationService();
	}

	private Dialect getDialect() {
		return getDatabase().getJdbcEnvironment().getDialect();
	}

	@Override
	public UUID getUUID() {
		return null;
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return options;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public PersistenceUnitMetadata getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return bootstrapContext.getTypeConfiguration();
	}

	@Override
	public SqmFunctionRegistry getFunctionRegistry() {
		return bootstrapContext.getFunctionRegistry();
	}

	@Override
	public Database getDatabase() {
		// important to delay this instantiation until as late as possible.
		if ( database == null ) {
			database = new Database( options );
		}
		return database;
	}

	@Override
	public NamedObjectRepository buildNamedQueryRepository() {
		throw new UnsupportedOperationException( "#buildNamedQueryRepository should not be called on InFlightMetadataCollector" );
	}

	@Override
	public Map<String, SqmFunctionDescriptor> getSqlFunctionMap() {
		return sqlFunctionMap;
	}

	@Override
	public Set<String> getContributors() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void orderColumns(boolean forceOrdering) {
		// nothing to do
	}

	@Override
	public void validate() throws MappingException {
		// nothing to do
	}

	@Override
	public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
		return new HashSet<>( mappedSuperClasses.values() );
	}

	@Override
	public void initSessionFactory(SessionFactoryImplementor sessionFactory) {
		throw new UnsupportedOperationException(
				"""
				You should not be building a SessionFactory from an in-flight metadata collector; \
				and of course we should better segment this in the API :)
				"""
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Composite handling

	@Override
	public void registerComponent(Component component) {
		composites.add( component );
	}

	@Override
	public void visitRegisteredComponents(Consumer<Component> consumer) {
		composites.forEach( consumer );
	}

	@Override
	public void registerGenericComponent(Component component) {
		genericComponentsMap.put( component.getComponentClass(), component );
	}

	@Override
	public Component getGenericComponent(Class<?> componentClass) {
		return genericComponentsMap.get( componentClass );
	}

	@Override
	public void registerEmbeddableSubclass(ClassDetails superclass, ClassDetails subclass) {
		embeddableSubtypes.computeIfAbsent( superclass, c -> new ArrayList<>() ).add( subclass );
	}

	@Override
	public List<ClassDetails> getEmbeddableSubclasses(ClassDetails superclass) {
		final List<ClassDetails> subclasses = embeddableSubtypes.get( superclass );
		return subclasses != null ? subclasses : emptyList();
	}

	@Override
	public DiscriminatorType<?> resolveEmbeddableDiscriminatorType(
			Class<?> embeddableClass,
			Supplier<DiscriminatorType<?>> supplier) {
		return embeddableDiscriminatorTypesMap.computeIfAbsent( embeddableClass, k -> supplier.get() );
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		throw new UnsupportedOperationException(
				"""
				You should not be building a SessionFactory from an in-flight metadata collector; \
				and of course we should better segment this in the API :)
				"""
		);
	}

	@Override
	public SessionFactoryImplementor buildSessionFactory() {
		throw new UnsupportedOperationException(
				"""
				You should not be building a SessionFactory from an in-flight metadata collector; \
				and of course we should better segment this in the API :)
				"""
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity handling

	@Override
	public java.util.Collection<PersistentClass> getEntityBindings() {
		return entityBindingMap.values();
	}

	@Override
	public Map<String, PersistentClass> getEntityBindingMap() {
		return entityBindingMap;
	}

	@Override
	public PersistentClass getEntityBinding(String entityName) {
		return entityBindingMap.get( entityName );
	}

	@Override
	public void addEntityBinding(PersistentClass persistentClass) throws DuplicateMappingException {
		final String entityName = persistentClass.getEntityName();
		final String jpaEntityName = persistentClass.getJpaEntityName();
		if ( entityBindingMap.containsKey( entityName ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, entityName );
		}

		for ( var existingPersistentClass : entityBindingMap.values() ) {
			if ( existingPersistentClass.getJpaEntityName().equals( jpaEntityName ) ) {
				throw new DuplicateMappingException(
						String.format(
								"Entity classes [%s] and [%s] share the entity name '%s' (entity names must be distinct)",
								existingPersistentClass.getClassName(),
								persistentClass.getClassName(),
								jpaEntityName
						),
						DuplicateMappingException.Type.ENTITY,
						jpaEntityName
				);
			}
		}

		entityBindingMap.put( entityName, persistentClass );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection handling

	@Override
	public java.util.Collection<Collection> getCollectionBindings() {
		return collectionBindingMap.values();
	}

	@Override
	public Collection getCollectionBinding(String role) {
		return collectionBindingMap.get( role );
	}

	@Override
	public void addCollectionBinding(Collection collection) throws DuplicateMappingException {
		final String collectionRole = collection.getRole();
		if ( collectionBindingMap.containsKey( collectionRole ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.COLLECTION, collectionRole );
		}
		collectionBindingMap.put( collectionRole, collection );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate Type handling


	@Override
	public TypeDefinitionRegistry getTypeDefinitionRegistry() {
		return typeDefRegistry;
	}

	@Override
	public TypeDefinition getTypeDefinition(String registrationKey) {
		return typeDefRegistry.resolve( registrationKey );
	}

	@Override
	public void addTypeDefinition(TypeDefinition typeDefinition) {
		typeDefRegistry.register( typeDefinition );
	}

	@Override
	public void registerValueMappingResolver(Function<MetadataBuildingContext, Boolean> resolver) {
		if ( valueResolvers == null ) {
			valueResolvers = new ArrayList<>();
		}
		valueResolvers.add( resolver );
	}

	@Override
	public void addJavaTypeRegistration(Class<?> javaType, JavaType<?> jtd) {
		getTypeConfiguration().getJavaTypeRegistry().addBaselineDescriptor( javaType, jtd );
	}

	@Override
	public void addJdbcTypeRegistration(int typeCode, JdbcType jdbcType) {
		getTypeConfiguration().getJdbcTypeRegistry().addDescriptor( typeCode, jdbcType );
	}

	private Map<Class<?>, Class<? extends EmbeddableInstantiator>> registeredInstantiators;

	@Override
	public void registerEmbeddableInstantiator(Class<?> embeddableType, Class<? extends EmbeddableInstantiator> instantiator) {
		if ( registeredInstantiators == null ) {
			registeredInstantiators = new HashMap<>();
		}
		registeredInstantiators.put( embeddableType, instantiator );
	}

	@Override
	public Class<? extends EmbeddableInstantiator> findRegisteredEmbeddableInstantiator(Class<?> embeddableType) {
		return registeredInstantiators == null ? null : registeredInstantiators.get( embeddableType );

	}

	private Map<Class<?>, Class<? extends CompositeUserType<?>>> registeredCompositeUserTypes;

	@Override
	public void registerCompositeUserType(Class<?> embeddableType, Class<? extends CompositeUserType<?>> userType) {
		if ( registeredCompositeUserTypes == null ) {
			registeredCompositeUserTypes = new HashMap<>();
		}
		registeredCompositeUserTypes.put( embeddableType, userType );
	}

	@Override
	public Class<? extends CompositeUserType<?>> findRegisteredCompositeUserType(Class<?> embeddableType) {
		return registeredCompositeUserTypes == null ? null : registeredCompositeUserTypes.get( embeddableType );

	}

	private Map<Class<?>, Class<? extends UserType<?>>> registeredUserTypes;
	@Override
	public void registerUserType(Class<?> basicType, Class<? extends UserType<?>> userType) {
		if ( registeredUserTypes == null ) {
			registeredUserTypes = new HashMap<>();
		}
		registeredUserTypes.put( basicType, userType );
	}

	@Override
	public Class<? extends UserType<?>> findRegisteredUserType(Class<?> basicType) {
		return registeredUserTypes == null ? null : registeredUserTypes.get( basicType );

	}

	private Map<CollectionClassification, CollectionTypeRegistrationDescriptor> collectionTypeRegistrations;

	@Override
	public void addCollectionTypeRegistration(CollectionTypeRegistration registrationAnnotation) {
		addCollectionTypeRegistration( registrationAnnotation.classification(),
				toDescriptor( registrationAnnotation ) );
	}

	@Override
	public void addCollectionTypeRegistration(
			CollectionClassification classification, CollectionTypeRegistrationDescriptor descriptor) {
		if ( collectionTypeRegistrations == null ) {
			collectionTypeRegistrations = new HashMap<>();
		}
		collectionTypeRegistrations.put( classification, descriptor );
	}

	@Override
	public CollectionTypeRegistrationDescriptor findCollectionTypeRegistration(CollectionClassification classification) {
		return collectionTypeRegistrations == null ? null : collectionTypeRegistrations.get( classification );

	}

	private CollectionTypeRegistrationDescriptor toDescriptor(CollectionTypeRegistration registrationAnnotation) {
		return new CollectionTypeRegistrationDescriptor( registrationAnnotation.type(),
				extractParameters( registrationAnnotation.parameters() ) );
	}

	private Map<String,String> extractParameters(Parameter[] annotationUsages) {
		if ( isEmpty( annotationUsages ) ) {
			return null;
		}
		else {
			final Map<String, String> result = mapOfSize( annotationUsages.length );
			for ( Parameter parameter : annotationUsages ) {
				result.put( parameter.name(), parameter.value() );
			}
			return result;
		}
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// attribute converters


	@Override
	public ConverterRegistry getConverterRegistry() {
		return this;
	}

	public AttributeConverterManager getAttributeConverterManager() {
		return attributeConverterManager;
	}

	private ClassmateContext getClassmateContext() {
		return getBootstrapContext().getClassmateContext();
	}

	@Override
	public void addAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
		attributeConverterManager.addConverter(
				ConverterDescriptors.of( converterClass, null, false, getClassmateContext() ) );
	}

	@Override
	public void addOverridableConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
		attributeConverterManager.addConverter(
				ConverterDescriptors.of( converterClass, null, true, getClassmateContext() ) );
	}

	@Override
	public void addAttributeConverter(ConverterDescriptor<?,?> descriptor) {
		attributeConverterManager.addConverter( descriptor );
	}

	@Override
	public void addRegisteredConversion(RegisteredConversion conversion) {
		attributeConverterManager.addRegistration( conversion, bootstrapContext );
	}

	@Override
	public ConverterAutoApplyHandler getAttributeConverterAutoApplyHandler() {
		return attributeConverterManager;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// filter definitions

	@Override
	public Map<String, FilterDefinition> getFilterDefinitions() {
		return filterDefinitionMap;
	}

	@Override
	public FilterDefinition getFilterDefinition(String name) {
		return filterDefinitionMap.get( name );
	}

	@Override
	public void addFilterDefinition(FilterDefinition filterDefinition) {
		if ( filterDefinition == null || filterDefinition.getFilterName() == null ) {
			throw new IllegalArgumentException( "Filter definition object or name is null: "  + filterDefinition );
		}
		filterDefinitionMap.put( filterDefinition.getFilterName(), filterDefinition );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// fetch profiles

	@Override
	public java.util.Collection<FetchProfile> getFetchProfiles() {
		return fetchProfileMap.values();
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return fetchProfileMap.get( name );
	}

	@Override
	public void addFetchProfile(FetchProfile profile) {
		if ( profile == null || profile.getName() == null ) {
			throw new IllegalArgumentException( "Fetch profile object or name is null: " + profile );
		}
		final FetchProfile old = fetchProfileMap.put( profile.getName(), profile );
		if ( old != null ) {
			log.warn( "Duplicated fetch profile with same name [" + profile.getName() + "] found." );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// identifier generators

	@Override
	public IdentifierGeneratorDefinition getIdentifierGenerator(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid generator name" );
		}
		return idGeneratorDefinitionMap.get( name );
	}

	@Override
	public java.util.Collection<Table> collectTableMappings() {
		final ArrayList<Table> tables = new ArrayList<>();
		for ( Namespace namespace : getDatabase().getNamespaces() ) {
			tables.addAll( namespace.getTables() );
		}
		return tables;
	}

	@Override
	public void addIdentifierGenerator(IdentifierGeneratorDefinition generator) {
		if ( generator == null || generator.getName() == null ) {
			throw new IllegalArgumentException( "ID generator object or name is null." );
		}
		else if ( !generator.getName().isEmpty()
			&& !defaultIdentifierGeneratorNames.contains( generator.getName() ) ) {
			final IdentifierGeneratorDefinition old =
					idGeneratorDefinitionMap.put( generator.getName(), generator );
			if ( old != null && !old.equals( generator ) ) {
				if ( bootstrapContext.getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
					throw new IllegalArgumentException( "Duplicate generator name " + old.getName()
							+ "; you will likely want to set the property "
							+ AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE + " to false " );
				}
				else {
					log.duplicateGeneratorName( old.getName() );
				}
			}
		}

	}

	@Override
	public void addDefaultIdentifierGenerator(IdentifierGeneratorDefinition generator) {
		addIdentifierGenerator( generator );
		defaultIdentifierGeneratorNames.add( generator.getName() );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named EntityGraph handling

	@Override
	public NamedEntityGraphDefinition getNamedEntityGraph(String name) {
		return namedEntityGraphMap.get( name );
	}

	@Override
	public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs() {
		return namedEntityGraphMap;
	}

	@Override
	public void addNamedEntityGraph(NamedEntityGraphDefinition definition) {
		final String name = definition.name();
		final NamedEntityGraphDefinition previous = namedEntityGraphMap.put( name, definition );
		if ( previous != null ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.NAMED_ENTITY_GRAPH, name );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named query handling

	public NamedHqlQueryDefinition<?> getNamedHqlQueryMapping(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid query name" );
		}
		return namedQueryMap.get( name );
	}

	@Override
	public void visitNamedHqlQueryDefinitions(Consumer<NamedHqlQueryDefinition<?>> definitionConsumer) {
		namedQueryMap.values().forEach( definitionConsumer );
	}

	@Override
	public void addNamedQuery(NamedHqlQueryDefinition<?> def) {
		if ( def == null ) {
			throw new IllegalArgumentException( "Named query definition is null" );
		}
		else if ( def.getRegistrationName() == null ) {
			throw new IllegalArgumentException( "Named query definition name is null: " + def.getHqlString() );
		}
		else if ( !defaultNamedQueryNames.contains( def.getRegistrationName() ) ) {
			applyNamedQuery( def.getRegistrationName(), def );
		}
	}

	private void applyNamedQuery(String name, NamedHqlQueryDefinition<?> query) {
		checkQueryName( name );
		namedQueryMap.put( name.intern(), query );
	}

	private void checkQueryName(String name) throws DuplicateMappingException {
		if ( namedQueryMap.containsKey( name ) || namedNativeQueryMap.containsKey( name ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.QUERY, name );
		}
	}

	@Override
	public void addDefaultQuery(NamedHqlQueryDefinition<?> queryDefinition) {
		applyNamedQuery( queryDefinition.getRegistrationName(), queryDefinition );
		defaultNamedQueryNames.add( queryDefinition.getRegistrationName() );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named native-query handling

	@Override
	public NamedNativeQueryDefinition<?> getNamedNativeQueryMapping(String name) {
		return namedNativeQueryMap.get( name );
	}

	@Override
	public void visitNamedNativeQueryDefinitions(Consumer<NamedNativeQueryDefinition<?>> definitionConsumer) {
		namedNativeQueryMap.values().forEach( definitionConsumer );
	}

	@Override
	public void addNamedNativeQuery(NamedNativeQueryDefinition<?> def) {
		if ( def == null ) {
			throw new IllegalArgumentException( "Named native query definition object is null" );
		}
		else if ( def.getRegistrationName() == null ) {
			throw new IllegalArgumentException( "Named native query definition name is null: " + def.getSqlQueryString() );
		}
		else if ( !defaultNamedNativeQueryNames.contains( def.getRegistrationName() ) ) {
			applyNamedNativeQuery( def.getRegistrationName(), def );
		}
	}

	private void applyNamedNativeQuery(String name, NamedNativeQueryDefinition<?> query) {
		checkQueryName( name );
		namedNativeQueryMap.put( name.intern(), query );
	}

	@Override
	public void addDefaultNamedNativeQuery(NamedNativeQueryDefinition<?> query) {
		applyNamedNativeQuery( query.getRegistrationName(), query );
		defaultNamedNativeQueryNames.add( query.getRegistrationName() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named stored-procedure handling


	@Override
	public NamedProcedureCallDefinition getNamedProcedureCallMapping(String name) {
		return namedProcedureCallMap.get( name );
	}

	@Override
	public void visitNamedProcedureCallDefinition(Consumer<NamedProcedureCallDefinition> definitionConsumer) {
		namedProcedureCallMap.values().forEach( definitionConsumer );
	}

	@Override
	public void addNamedProcedureCallDefinition(NamedProcedureCallDefinition definition) {
		if ( definition == null ) {
			throw new IllegalArgumentException( "Named query definition is null" );
		}

		final String name = definition.getRegistrationName();
		if ( !defaultNamedProcedureNames.contains( name ) ) {
			final NamedProcedureCallDefinition previous = namedProcedureCallMap.put( name, definition );
			if ( previous != null ) {
				throw new DuplicateMappingException( DuplicateMappingException.Type.PROCEDURE, name );
			}
		}
	}

	@Override
	public void addDefaultNamedProcedureCall(NamedProcedureCallDefinitionImpl definition) {
		addNamedProcedureCallDefinition( definition );
		defaultNamedProcedureNames.add( definition.getRegistrationName() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// result-set mapping handling

	@Override
	public NamedResultSetMappingDescriptor getResultSetMapping(String name) {
		return sqlResultSetMappingMap.get( name );
	}

	@Override
	public void visitNamedResultSetMappingDefinition(Consumer<NamedResultSetMappingDescriptor> definitionConsumer) {
		sqlResultSetMappingMap.values().forEach( definitionConsumer );
	}

	@Override
	public void addResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDescriptor) {
		if ( resultSetMappingDescriptor == null ) {
			throw new IllegalArgumentException( "Result-set mapping was null" );
		}

		final String name = resultSetMappingDescriptor.getRegistrationName();
		if ( name == null ) {
			throw new IllegalArgumentException( "Result-set mapping name is null: " + resultSetMappingDescriptor );
		}
		else if ( !defaultSqlResultSetMappingNames.contains( name ) ) {
			applyResultSetMapping( resultSetMappingDescriptor );
		}
	}

	public void applyResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDescriptor) {
		final NamedResultSetMappingDescriptor old = sqlResultSetMappingMap.put(
				resultSetMappingDescriptor.getRegistrationName(),
				resultSetMappingDescriptor
		);
		if ( old != null ) {
			throw new DuplicateMappingException(
					DuplicateMappingException.Type.RESULT_SET_MAPPING,
					resultSetMappingDescriptor.getRegistrationName()
			);
		}
	}

	@Override
	public void addDefaultResultSetMapping(NamedResultSetMappingDescriptor definition) {
		final String name = definition.getRegistrationName();
		if ( !defaultSqlResultSetMappingNames.contains( name ) ) {
			sqlResultSetMappingMap.remove( name );
		}
		applyResultSetMapping( definition );
		defaultSqlResultSetMappingNames.add( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// imports

	@Override
	public Map<String,String> getImports() {
		return imports;
	}

	@Override
	public void addImport(String importName, String className) {
		if ( importName == null || className == null ) {
			throw new IllegalArgumentException( "Import name or entity name is null" );
		}
		log.tracev( "Import: {0} -> {1}", importName, className);
		String old = imports.put( importName, className);
		if ( old != null ) {
			log.debugf( "import name [%s] overrode previous [{%s}]", importName, old );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Table handling

	@Override
	public Table addTable(
			String schemaName,
			String catalogName,
			String name,
			String subselectFragment,
			boolean isAbstract,
			MetadataBuildingContext buildingContext) {
		final Namespace namespace = getDatabase().locateNamespace(
				getDatabase().toIdentifier( catalogName ),
				getDatabase().toIdentifier( schemaName )
		);

		// annotation binding depends on the "table name" for @Subselect bindings
		// being set into the generated table (mainly to avoid later NPE), but for now we need to keep that :(
		final Identifier logicalName = name != null ? getDatabase().toIdentifier( name ) : null;

		if ( subselectFragment != null ) {
			return new Table( buildingContext.getCurrentContributorName(),
					namespace, logicalName, subselectFragment, isAbstract );
		}
		else {
			final Table existing = namespace.locateTable( logicalName );
			if ( existing != null ) {
				if ( !isAbstract ) {
					existing.setAbstract( false );
				}
				return existing;
			}

			return namespace.createTable(
					logicalName,
					(physicalName) ->
							new Table( buildingContext.getCurrentContributorName(),
									namespace, physicalName, isAbstract )
			);
		}
	}

	@Override
	public Table addDenormalizedTable(
			String schemaName,
			String catalogName,
			String name,
			boolean isAbstract,
			String subselectFragment,
			Table includedTable,
			MetadataBuildingContext buildingContext) throws DuplicateMappingException {
		final Database db = getDatabase();
		final Namespace namespace =
				db.locateNamespace( db.toIdentifier( catalogName ), db.toIdentifier( schemaName ) );

		// annotation binding depends on the "table name" for @Subselect bindings
		// being set into the generated table (mainly to avoid later NPE), but for now we need to keep that :(
		final Identifier logicalName = name != null ? db.toIdentifier( name ) : null;

		if ( subselectFragment != null ) {
			return namespace.createDenormalizedTable(
					logicalName,
					(physicalName) -> new DenormalizedTable(
							buildingContext.getCurrentContributorName(),
							namespace,
							logicalName,
							subselectFragment,
							isAbstract,
							includedTable
					)
			);
		}
		else {
			if ( namespace.locateTable( logicalName ) != null ) {
				assert logicalName != null;
				throw new DuplicateMappingException( DuplicateMappingException.Type.TABLE, logicalName.toString() );
			}
			else {
				return namespace.createDenormalizedTable(
						logicalName,
						(physicalTableName) -> new DenormalizedTable(
								buildingContext.getCurrentContributorName(),
								namespace,
								physicalTableName,
								isAbstract,
								includedTable
						)
				);
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mapping impl

	@Override
	public org.hibernate.type.Type getIdentifierType(String entityName) throws MappingException {
		final PersistentClass pc = entityBindingMap.get( entityName );
		if ( pc == null ) {
			throw new MappingException( "persistent class not known: " + entityName );
		}
		return pc.getIdentifier().getType();
	}

	@Override
	public String getIdentifierPropertyName(String entityName) throws MappingException {
		final PersistentClass persistentClass = entityBindingMap.get( entityName );
		if ( persistentClass == null ) {
			throw new MappingException( "persistent class not known: " + entityName );
		}
		return persistentClass.hasIdentifierProperty()
				? persistentClass.getIdentifierProperty().getName()
				: null;
	}

	@Override
	public org.hibernate.type.Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
		final PersistentClass persistentClass = entityBindingMap.get( entityName );
		if ( persistentClass == null ) {
			throw new MappingException( "Persistent class not known: " + entityName );
		}
		final Property prop = persistentClass.getReferencedProperty( propertyName );
		if ( prop == null ) {
			throw new MappingException( "Property not known: " + entityName + '.' + propertyName );
		}
		return prop.getType();
	}


	private final Map<Identifier,Identifier> logicalToPhysicalTableNameMap = new HashMap<>();
	private final Map<Identifier,Identifier> physicalToLogicalTableNameMap = new HashMap<>();

	@Override
	public void addTableNameBinding(Identifier logicalName, Table table) {
		logicalToPhysicalTableNameMap.put( logicalName, table.getNameIdentifier() );
		physicalToLogicalTableNameMap.put( table.getNameIdentifier(), logicalName );
	}

	@Override
	public void addTableNameBinding(String schema, String catalog, String logicalName, String realTableName, Table denormalizedSuperTable) {
		final Identifier logicalNameIdentifier = getDatabase().toIdentifier( logicalName );
		final Identifier physicalNameIdentifier = getDatabase().toIdentifier( realTableName );

		logicalToPhysicalTableNameMap.put( logicalNameIdentifier, physicalNameIdentifier );
		physicalToLogicalTableNameMap.put( physicalNameIdentifier, logicalNameIdentifier );
	}

	@Override
	public String getLogicalTableName(Table ownerTable) {
		final Identifier logicalName = physicalToLogicalTableNameMap.get( ownerTable.getNameIdentifier() );
		if ( logicalName == null ) {
			throw new MappingException( "Unable to find physical table: " + ownerTable.getName() );
		}
		return logicalName.render();
	}

	@Override
	public String getPhysicalTableName(Identifier logicalName) {
		final Identifier physicalName = logicalToPhysicalTableNameMap.get( logicalName );
		return physicalName == null ? null : physicalName.render();
	}

	@Override
	public String getPhysicalTableName(String logicalName) {
		return getPhysicalTableName( getDatabase().toIdentifier( logicalName ) );
	}

	/**
	 * Internal struct used to maintain xref between physical and logical column
	 * names for a table.  Mainly this is used to ensure that the defined NamingStrategy
	 * is not creating duplicate column names.
	 */
	private class TableColumnNameBinding implements Serializable {
		private final String tableName;
		private final Map<Identifier, String> logicalToPhysical = new HashMap<>();
		private final Map<String, Identifier> physicalToLogical = new HashMap<>();

		private TableColumnNameBinding(String tableName) {
			this.tableName = tableName;
		}

		public void addBinding(Identifier logicalName, Column physicalColumn) {
			final String physicalNameString = physicalColumn.getQuotedName( getDialect() );
			bindLogicalToPhysical( logicalName, physicalNameString );
			bindPhysicalToLogical( logicalName, physicalNameString );
		}

		private void bindLogicalToPhysical(Identifier logicalName, String physicalName) throws DuplicateMappingException {
			final String existingPhysicalNameMapping = logicalToPhysical.put( logicalName, physicalName );
			if ( existingPhysicalNameMapping != null ) {
				final boolean areSame = logicalName.isQuoted()
						? physicalName.equals( existingPhysicalNameMapping )
						: physicalName.equalsIgnoreCase( existingPhysicalNameMapping );
				if ( !areSame ) {
					throw new DuplicateMappingException(
							String.format(
									Locale.ENGLISH,
									"Table [%s] contains logical column name [%s] referring to multiple physical " +
											"column names: [%s], [%s]",
									tableName,
									logicalName,
									existingPhysicalNameMapping,
									physicalName
							),
							DuplicateMappingException.Type.COLUMN_BINDING,
							tableName + "." + logicalName
					);
				}
			}
		}

		private void bindPhysicalToLogical(Identifier logicalName, String physicalName) throws DuplicateMappingException {
			final Identifier existingLogicalName = physicalToLogical.put( physicalName, logicalName );
			if ( existingLogicalName != null && ! existingLogicalName.equals( logicalName ) ) {
				throw new DuplicateMappingException(
						String.format(
								Locale.ENGLISH,
								"Table [%s] contains physical column name [%s] referred to by multiple logical " +
										"column names: [%s], [%s]",
								tableName,
								physicalName,
								logicalName,
								existingLogicalName
						),
						DuplicateMappingException.Type.COLUMN_BINDING,
						tableName + "." + physicalName
				);
			}
		}
	}

	private Map<Table,TableColumnNameBinding> columnNameBindingByTableMap;

	@Override
	public void addColumnNameBinding(Table table, String logicalName, Column column) throws DuplicateMappingException {
		addColumnNameBinding( table, getDatabase().toIdentifier( logicalName ), column );
	}

	@Override
	public void addColumnNameBinding(Table table, Identifier logicalName, Column column) throws DuplicateMappingException {
		TableColumnNameBinding binding;

		if ( columnNameBindingByTableMap == null ) {
			columnNameBindingByTableMap = new HashMap<>();
			binding = null;
		}
		else {
			binding = columnNameBindingByTableMap.get( table );
		}

		if ( binding == null ) {
			binding = new TableColumnNameBinding( table.getName() );
			columnNameBindingByTableMap.put( table, binding );
		}

		binding.addBinding( logicalName, column );
	}

	@Override
	public String getPhysicalColumnName(Table table, String logicalName) throws MappingException {
		final Identifier identifier = getDatabase().toIdentifier( logicalName );
		if ( identifier == null ) {
			throw new MappingException( String.format(
					Locale.ENGLISH,
					"Column with logical name '%s' in table '%s' cannot be mapped to column identifier",
					logicalName,
					table.getName()
			) );
		}
		return getPhysicalColumnName( table, identifier );
	}

	@Override
	public String getPhysicalColumnName(Table table, Identifier logicalName) throws MappingException {
		if ( logicalName == null ) {
			throw new MappingException( "Logical column name cannot be null" );
		}

		Table currentTable = table;
		while ( currentTable != null ) {
			final TableColumnNameBinding binding = columnNameBindingByTableMap.get( currentTable );
			if ( binding != null ) {
				final String physicalName = binding.logicalToPhysical.get( logicalName );
				if ( physicalName != null ) {
					return physicalName;
				}
			}
			currentTable =
					currentTable instanceof DenormalizedTable denormalizedTable
							? denormalizedTable.getIncludedTable()
							: null;
		}

		assert table != null;
		throw new MappingException( "Unable to find column with logical name " + logicalName.render()
				+ " in table " + table.getName() );
	}

	@Override
	public String getLogicalColumnName(Table table, String physicalName) throws MappingException {
		return getLogicalColumnName( table, getDatabase().toIdentifier( physicalName ) );
	}


	@Override
	public String getLogicalColumnName(Table table, Identifier physicalName) throws MappingException {
		final String physicalNameString = physicalName.render( getDialect() );
		Identifier logicalName = null;
		Table currentTable = table;
		while ( currentTable != null ) {
			final TableColumnNameBinding binding = columnNameBindingByTableMap.get( currentTable );
			if ( binding != null ) {
				logicalName = binding.physicalToLogical.get( physicalNameString );
				if ( logicalName != null ) {
					break;
				}
			}
			currentTable =
					currentTable instanceof DenormalizedTable denormalizedTable
							? denormalizedTable.getIncludedTable()
							: null;
		}

		if ( logicalName == null ) {
			assert table != null;
			throw new MappingException( "Unable to find column with physical name '"
					+ physicalNameString + "' in table '" + table.getName() + "'" );
		}

		return logicalName.render();
	}

	@Override
	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		getDatabase().addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
	}

	private final Map<String,AnnotatedClassType> annotatedClassTypeMap = new HashMap<>();

	@Override
	public AnnotatedClassType getClassType(ClassDetails clazz) {
		final AnnotatedClassType type = annotatedClassTypeMap.get( clazz.getName() );
		return type == null ? addClassType( clazz ) : type;
	}

	@Override
	public AnnotatedClassType addClassType(ClassDetails clazz) {
		final AnnotatedClassType type = getAnnotatedClassType(clazz);
		annotatedClassTypeMap.put( clazz.getName(), type );
		return type;
	}

	private static AnnotatedClassType getAnnotatedClassType(ClassDetails clazz) {
		if ( clazz.hasDirectAnnotationUsage( Entity.class ) ) {
			if ( clazz.hasDirectAnnotationUsage( Embeddable.class ) ) {
				throw new AnnotationException( "Invalid class annotated both '@Entity' and '@Embeddable': '" + clazz.getName() + "'" );
			}
			else if ( clazz.hasDirectAnnotationUsage( jakarta.persistence.MappedSuperclass.class ) ) {
				throw new AnnotationException( "Invalid class annotated both '@Entity' and '@MappedSuperclass': '" + clazz.getName() + "'" );
			}
			return AnnotatedClassType.ENTITY;
		}
		else if ( clazz.hasDirectAnnotationUsage( Embeddable.class ) ) {
			if ( clazz.hasDirectAnnotationUsage( jakarta.persistence.MappedSuperclass.class ) ) {
				throw new AnnotationException( "Invalid class annotated both '@Embeddable' and '@MappedSuperclass': '" + clazz.getName() + "'" );
			}
			return AnnotatedClassType.EMBEDDABLE;
		}
		else if ( clazz.hasDirectAnnotationUsage( jakarta.persistence.MappedSuperclass.class ) ) {
			return AnnotatedClassType.MAPPED_SUPERCLASS;
		}
		else if ( clazz.hasDirectAnnotationUsage( Imported.class ) ) {
			return AnnotatedClassType.IMPORTED;
		}
		else {
			return AnnotatedClassType.NONE;
		}
	}

	@Override
	public void addMappedSuperclass(Class<?> type, MappedSuperclass mappedSuperclass) {
		if ( mappedSuperClasses == null ) {
			mappedSuperClasses = new HashMap<>();
		}
		mappedSuperClasses.put( type, mappedSuperclass );
	}

	@Override
	public MappedSuperclass getMappedSuperclass(Class<?> type) {
		return mappedSuperClasses == null ? null : mappedSuperClasses.get( type );
	}

	@Override
	public PropertyData getPropertyAnnotatedWithMapsId(ClassDetails entityType, String propertyName) {
		if ( propertiesAnnotatedWithMapsId == null ) {
			return null;
		}
		else {
			final var map = propertiesAnnotatedWithMapsId.get( entityType );
			return map == null ? null : map.get( propertyName );
		}
	}

	@Override
	public void addPropertyAnnotatedWithMapsId(ClassDetails entityType, PropertyData property) {
		if ( propertiesAnnotatedWithMapsId == null ) {
			propertiesAnnotatedWithMapsId = new HashMap<>();
		}
		propertiesAnnotatedWithMapsId.computeIfAbsent( entityType, k -> new HashMap<>() )
				.put( property.getAttributeMember().getDirectAnnotationUsage( MapsId.class ).value(), property );
	}

	@Override
	public PropertyData getPropertyAnnotatedWithIdAndToOne(ClassDetails entityType, String propertyName) {
		if ( propertiesAnnotatedWithIdAndToOne == null ) {
			return null;
		}
		else {
			final var map = propertiesAnnotatedWithIdAndToOne.get( entityType );
			return map == null ? null : map.get( propertyName );
		}
	}

	@Override
	public void addToOneAndIdProperty(ClassDetails entityType, PropertyData property) {
		if ( propertiesAnnotatedWithIdAndToOne == null ) {
			propertiesAnnotatedWithIdAndToOne = new HashMap<>();
		}
		propertiesAnnotatedWithIdAndToOne.computeIfAbsent( entityType, k -> new HashMap<>() )
				.put( property.getPropertyName(), property );
	}

	@Override
	public void addMappedBy(String entityName, String propertyName, String inversePropertyName) {
		if ( mappedByResolver == null ) {
			mappedByResolver = new HashMap<>();
		}
		mappedByResolver.put( entityName + "." + propertyName, inversePropertyName );
	}

	@Override
	public String getFromMappedBy(String entityName, String propertyName) {
		return mappedByResolver == null ? null : mappedByResolver.get( entityName + "." + propertyName );
	}

	@Override
	public void addPropertyReferencedAssociation(String entityName, String propertyName, String propertyRef) {
		if ( propertyRefResolver == null ) {
			propertyRefResolver = new HashMap<>();
		}
		propertyRefResolver.put( entityName + "." + propertyName, propertyRef );
	}

	@Override
	public String getPropertyReferencedAssociation(String entityName, String propertyName) {
		return propertyRefResolver == null ? null : propertyRefResolver.get( entityName + "." + propertyName );
	}

	private static class DelayedPropertyReferenceHandlerAnnotationImpl implements DelayedPropertyReferenceHandler {
		public final String referencedClass;
		public final String propertyName;
		public final boolean unique;

		public DelayedPropertyReferenceHandlerAnnotationImpl(String referencedClass, String propertyName, boolean unique) {
			this.referencedClass = referencedClass;
			this.propertyName = propertyName;
			this.unique = unique;
		}

		@Override
		public void process(InFlightMetadataCollector metadataCollector) {
			final PersistentClass clazz = metadataCollector.getEntityBinding( referencedClass );
			if ( clazz == null ) {
				throw new MappingException( "property-ref to unmapped class: " + referencedClass );
			}

			final Property prop = clazz.getReferencedProperty( propertyName );
			if ( unique ) {
				( (SimpleValue) prop.getValue() ).setAlternateUniqueKey( true );
			}
		}
	}

	@Override
	public void addPropertyReference(String referencedClass, String propertyName) {
		addDelayedPropertyReferenceHandler(
				new DelayedPropertyReferenceHandlerAnnotationImpl( referencedClass, propertyName, false )
		);
	}

	@Override
	public void addDelayedPropertyReferenceHandler(DelayedPropertyReferenceHandler handler) {
		if ( delayedPropertyReferenceHandlers == null ) {
			delayedPropertyReferenceHandlers = new HashSet<>();
		}
		delayedPropertyReferenceHandlers.add( handler );
	}

	@Override
	public void addUniquePropertyReference(String referencedClass, String propertyName) {
		addDelayedPropertyReferenceHandler(
				new DelayedPropertyReferenceHandlerAnnotationImpl( referencedClass, propertyName, true )
		);
	}

	private final Map<String,EntityTableXrefImpl> entityTableXrefMap = new HashMap<>();

	@Override
	public EntityTableXref getEntityTableXref(String entityName) {
		return entityTableXrefMap.get( entityName );
	}

	@Override
	public EntityTableXref addEntityTableXref(
			String entityName,
			Identifier primaryTableLogicalName,
			Table primaryTable,
			EntityTableXref superEntityTableXref) {
		final EntityTableXrefImpl entry = new EntityTableXrefImpl(
				primaryTableLogicalName,
				primaryTable,
				(EntityTableXrefImpl) superEntityTableXref
		);

		entityTableXrefMap.put( entityName, entry );

		return entry;
	}

	@Override
	public Map<String, Join> getJoins(String entityName) {
		final EntityTableXrefImpl xrefEntry = entityTableXrefMap.get( entityName );
		return xrefEntry == null ? null : xrefEntry.secondaryTableJoinMap;
	}

	private static final class EntityTableXrefImpl implements EntityTableXref {
		private final Identifier primaryTableLogicalName;
		private final Table primaryTable;
		private final EntityTableXrefImpl superEntityTableXref;

		//annotations needs a Map<String,Join>
		//private Map<Identifier,Join> secondaryTableJoinMap;
		private Map<String,Join> secondaryTableJoinMap;

		public EntityTableXrefImpl(
				Identifier primaryTableLogicalName, Table primaryTable, EntityTableXrefImpl superEntityTableXref) {
			this.primaryTableLogicalName = primaryTableLogicalName;
			this.primaryTable = primaryTable;
			this.superEntityTableXref = superEntityTableXref;
		}

		@Override
		public void addSecondaryTable(
				LocalMetadataBuildingContext buildingContext, Identifier logicalName, Join secondaryTableJoin) {
			if ( Identifier.areEqual( primaryTableLogicalName, logicalName ) ) {
				throw new org.hibernate.boot.MappingException(
						String.format(
								Locale.ENGLISH,
								"Attempt to add secondary table with same name as primary table [%s]",
								primaryTableLogicalName
						),
						buildingContext.getOrigin()
				);
			}


			if ( secondaryTableJoinMap == null ) {
				//secondaryTableJoinMap = new HashMap<Identifier,Join>();
				//secondaryTableJoinMap.put( logicalName, secondaryTableJoin );
				secondaryTableJoinMap = new HashMap<>();
				secondaryTableJoinMap.put( logicalName.getCanonicalName(), secondaryTableJoin );
			}
			else {
				//final Join existing = secondaryTableJoinMap.put( logicalName, secondaryTableJoin );
				final Join existing = secondaryTableJoinMap.put( logicalName.getCanonicalName(), secondaryTableJoin );

				if ( existing != null ) {
					throw new org.hibernate.boot.MappingException(
							String.format(
									Locale.ENGLISH,
									"Added secondary table with same name [%s]",
									logicalName
							),
							buildingContext.getOrigin()
					);
				}
			}
		}

		@Override
		public void addSecondaryTable(QualifiedTableName logicalQualifiedTableName, Join secondaryTableJoin) {
			final Identifier tableName = logicalQualifiedTableName.getTableName();

			if ( Identifier.areEqual(
				toIdentifier(
					new QualifiedTableName(
						toIdentifier( primaryTable.getCatalog() ),
						toIdentifier( primaryTable.getSchema() ),
						primaryTableLogicalName
					).render()
				),
				toIdentifier( logicalQualifiedTableName.render() ) ) ) {
				throw new DuplicateSecondaryTableException( tableName );
			}

			if ( secondaryTableJoinMap == null ) {
				//secondaryTableJoinMap = new HashMap<Identifier,Join>();
				//secondaryTableJoinMap.put( logicalName, secondaryTableJoin );
				secondaryTableJoinMap = new HashMap<>();
				secondaryTableJoinMap.put( tableName.getCanonicalName(), secondaryTableJoin );
			}
			else {
				//final Join existing = secondaryTableJoinMap.put( logicalName, secondaryTableJoin );
				final Join existing =
						secondaryTableJoinMap.put( tableName.getCanonicalName(), secondaryTableJoin );
				if ( existing != null ) {
					throw new DuplicateSecondaryTableException( tableName );
				}
			}
		}

		@Override
		public Table getPrimaryTable() {
			return primaryTable;
		}

		@Override
		public Table resolveTable(Identifier tableName) {
			if ( tableName == null ) {
				return primaryTable;
			}

			if ( Identifier.areEqual( primaryTableLogicalName, tableName ) ) {
				return primaryTable;
			}

			Join secondaryTableJoin = null;
			if ( secondaryTableJoinMap != null ) {
				//secondaryTableJoin = secondaryTableJoinMap.get( tableName );
				secondaryTableJoin = secondaryTableJoinMap.get( tableName.getCanonicalName() );
			}

			if ( secondaryTableJoin != null ) {
				return secondaryTableJoin.getTable();
			}

			if ( superEntityTableXref != null ) {
				return superEntityTableXref.resolveTable( tableName );
			}

			return null;
		}

		public Join locateJoin(Identifier tableName) {
			if ( tableName == null ) {
				return null;
			}

			Join join = null;
			if ( secondaryTableJoinMap != null ) {
				join = secondaryTableJoinMap.get( tableName.getCanonicalName() );
			}

			if ( join != null ) {
				return join;
			}

			if ( superEntityTableXref != null ) {
				return superEntityTableXref.locateJoin( tableName );
			}

			return null;
		}
	}

	private ArrayList<IdGeneratorResolver> idGeneratorResolverSecondPassList;
	private ArrayList<SetBasicValueTypeSecondPass> setBasicValueTypeSecondPassList;
	private ArrayList<AggregateComponentSecondPass> aggregateComponentSecondPassList;
	private ArrayList<FkSecondPass> fkSecondPassList;
	private ArrayList<CreateKeySecondPass> createKeySecondPassList;
	private ArrayList<ImplicitToOneJoinTableSecondPass> toOneJoinTableSecondPassList;
	private ArrayList<SecondaryTableSecondPass> secondaryTableSecondPassList;
	private ArrayList<SecondaryTableFromAnnotationSecondPass> secondaryTableFromAnnotationSecondPassesList;
	private ArrayList<QuerySecondPass> querySecondPassList;
	private ArrayList<ImplicitColumnNamingSecondPass> implicitColumnNamingSecondPassList;

	private ArrayList<SecondPass> generalSecondPassList;
	private ArrayList<OptionalDeterminationSecondPass> optionalDeterminationSecondPassList;

	@Override
	public void addSecondPass(SecondPass secondPass) {
		addSecondPass( secondPass, false );
	}

	@Override
	public void addSecondPass(SecondPass secondPass, boolean onTopOfTheQueue) {
		if ( secondPass instanceof IdGeneratorResolver generatorResolver ) {
			addIdGeneratorResolverSecondPass( generatorResolver, onTopOfTheQueue );
		}
		else if ( secondPass instanceof SetBasicValueTypeSecondPass setBasicValueTypeSecondPass ) {
			addSetBasicValueTypeSecondPass( setBasicValueTypeSecondPass, onTopOfTheQueue );
		}
		else if ( secondPass instanceof AggregateComponentSecondPass aggregateComponentSecondPass ) {
			addAggregateComponentSecondPass( aggregateComponentSecondPass, onTopOfTheQueue );
		}
		else if ( secondPass instanceof FkSecondPass fkSecondPass ) {
			addFkSecondPass( fkSecondPass, onTopOfTheQueue );
		}
		else if ( secondPass instanceof CreateKeySecondPass createKeySecondPass ) {
			addCreateKeySecondPass( createKeySecondPass, onTopOfTheQueue );
		}
		else if ( secondPass instanceof ImplicitToOneJoinTableSecondPass implicitToOneJoinTableSecondPass ) {
			addImplicitToOneJoinTableSecondPass( implicitToOneJoinTableSecondPass );
		}
		else if ( secondPass instanceof SecondaryTableSecondPass secondaryTableSecondPass ) {
			addSecondaryTableSecondPass( secondaryTableSecondPass, onTopOfTheQueue );
		}
		else if ( secondPass instanceof SecondaryTableFromAnnotationSecondPass secondaryTableFromAnnotationSecondPass ) {
			addSecondaryTableFromAnnotationSecondPass( secondaryTableFromAnnotationSecondPass, onTopOfTheQueue );
		}
		else if ( secondPass instanceof QuerySecondPass querySecondPass ) {
			addQuerySecondPass( querySecondPass, onTopOfTheQueue );
		}
		else if ( secondPass instanceof ImplicitColumnNamingSecondPass implicitColumnNamingSecondPass ) {
			addImplicitColumnNamingSecondPass( implicitColumnNamingSecondPass );
		}
		else if ( secondPass instanceof OptionalDeterminationSecondPass optionalDeterminationSecondPass ) {
			addOptionalDeterminationSecondPass( optionalDeterminationSecondPass );
		}
		else {
			// add to the general SecondPass list
			if ( generalSecondPassList == null ) {
				generalSecondPassList = new ArrayList<>();
			}
			addSecondPass( secondPass, generalSecondPassList, onTopOfTheQueue );
		}
	}

	private <T extends SecondPass> void addSecondPass(T secondPass, ArrayList<T> secondPassList, boolean onTopOfTheQueue) {
		if ( onTopOfTheQueue ) {
			secondPassList.add( 0, secondPass );
		}
		else {
			secondPassList.add( secondPass );
		}
	}

	private void addSetBasicValueTypeSecondPass(SetBasicValueTypeSecondPass secondPass, boolean onTopOfTheQueue) {
		if ( setBasicValueTypeSecondPassList == null ) {
			setBasicValueTypeSecondPassList = new ArrayList<>();
		}
		addSecondPass( secondPass, setBasicValueTypeSecondPassList, onTopOfTheQueue );
	}

	private void addAggregateComponentSecondPass(AggregateComponentSecondPass secondPass, boolean onTopOfTheQueue) {
		if ( aggregateComponentSecondPassList == null ) {
			aggregateComponentSecondPassList = new ArrayList<>();
		}
		addSecondPass( secondPass, aggregateComponentSecondPassList, onTopOfTheQueue );
	}

	private void addIdGeneratorResolverSecondPass(IdGeneratorResolver secondPass, boolean onTopOfTheQueue) {
		if ( idGeneratorResolverSecondPassList == null ) {
			idGeneratorResolverSecondPassList = new ArrayList<>();
		}
		addSecondPass( secondPass, idGeneratorResolverSecondPassList, onTopOfTheQueue );
	}

	private void addFkSecondPass(FkSecondPass secondPass, boolean onTopOfTheQueue) {
		if ( fkSecondPassList == null ) {
			fkSecondPassList = new ArrayList<>();
		}
		addSecondPass( secondPass, fkSecondPassList, onTopOfTheQueue );
	}

	private void addCreateKeySecondPass(CreateKeySecondPass secondPass, boolean onTopOfTheQueue) {
		if ( createKeySecondPassList == null ) {
			createKeySecondPassList = new ArrayList<>();
		}
		addSecondPass( secondPass, createKeySecondPassList, onTopOfTheQueue );
	}

	private void addImplicitToOneJoinTableSecondPass(ImplicitToOneJoinTableSecondPass secondPass) {
		if ( toOneJoinTableSecondPassList == null ) {
			toOneJoinTableSecondPassList = new ArrayList<>();
		}
		toOneJoinTableSecondPassList.add( secondPass );
	}

	private void addSecondaryTableSecondPass(SecondaryTableSecondPass secondPass, boolean onTopOfTheQueue) {
		if ( secondaryTableSecondPassList == null ) {
			secondaryTableSecondPassList = new ArrayList<>();
		}
		addSecondPass( secondPass, secondaryTableSecondPassList, onTopOfTheQueue );
	}

	private void addSecondaryTableFromAnnotationSecondPass(
			SecondaryTableFromAnnotationSecondPass secondPass, boolean onTopOfTheQueue){
		if ( secondaryTableFromAnnotationSecondPassesList == null ) {
			secondaryTableFromAnnotationSecondPassesList = new ArrayList<>();
		}
		addSecondPass( secondPass, secondaryTableFromAnnotationSecondPassesList, onTopOfTheQueue );
	}

	private void addQuerySecondPass(QuerySecondPass secondPass, boolean onTopOfTheQueue) {
		if ( querySecondPassList == null ) {
			querySecondPassList = new ArrayList<>();
		}
		addSecondPass( secondPass, querySecondPassList, onTopOfTheQueue );
	}

	private void addImplicitColumnNamingSecondPass(ImplicitColumnNamingSecondPass secondPass) {
		if ( implicitColumnNamingSecondPassList == null ) {
			implicitColumnNamingSecondPassList = new ArrayList<>();
		}
		implicitColumnNamingSecondPassList.add( secondPass );
	}

	private void addOptionalDeterminationSecondPass(OptionalDeterminationSecondPass secondPass) {
		if ( optionalDeterminationSecondPassList == null ) {
			optionalDeterminationSecondPassList = new ArrayList<>();
		}
		optionalDeterminationSecondPassList.add( secondPass );
	}


	private boolean inSecondPass = false;


	/**
	 * Ugh!  But we need this done before we ask Envers to produce its entities.
	 */
	public void processSecondPasses(MetadataBuildingContext buildingContext) {
		assert !inSecondPass;
		inSecondPass = true;

		try {
			processSecondPasses( idGeneratorResolverSecondPassList );
			processSecondPasses( implicitColumnNamingSecondPassList );
			processSecondPasses( setBasicValueTypeSecondPassList );
			processSecondPasses( toOneJoinTableSecondPassList );

			composites.forEach( Component::sortProperties );

			processFkSecondPassesInOrder();

			processSecondPasses(createKeySecondPassList);
			processSecondPasses( secondaryTableSecondPassList );

			processSecondPasses( querySecondPassList );
			processSecondPasses( generalSecondPassList );
			processSecondPasses( optionalDeterminationSecondPassList );

			processPropertyReferences();

			processSecondPasses( aggregateComponentSecondPassList );
			secondPassCompileForeignKeys( buildingContext );

			processNaturalIdUniqueKeyBinders();

			processCachingOverrides();

			processValueResolvers( buildingContext );
		}
		finally {
			inSecondPass = false;
		}
	}

	private void processValueResolvers(MetadataBuildingContext buildingContext) {
		if ( valueResolvers != null ) {
			while ( !valueResolvers.isEmpty() ) {
				final boolean anyRemoved =
						valueResolvers.removeIf( resolver -> resolver.apply( buildingContext ) );
				if ( !anyRemoved ) {
					throw new MappingException( "Unable to complete initialization of boot meta-model" );
				}
			}
		}
	}

	private void processSecondPasses(ArrayList<? extends SecondPass> secondPasses) {
		if ( secondPasses != null ) {
			for ( SecondPass secondPass : secondPasses ) {
				secondPass.doSecondPass( getEntityBindingMap() );
			}
			secondPasses.clear();
		}
	}

	private void processFkSecondPassesInOrder() {
		if ( fkSecondPassList == null || fkSecondPassList.isEmpty() ) {
			processSecondPasses( secondaryTableFromAnnotationSecondPassesList );
		}
		else {
			// split FkSecondPass instances into primary key and non primary key FKs.
			// While doing so build a map of class names to FkSecondPass instances depending on this class.
			final Map<String, Set<FkSecondPass>> isADependencyOf = new HashMap<>();
			final List<FkSecondPass> endOfQueueFkSecondPasses = new ArrayList<>( fkSecondPassList.size() );
			for ( FkSecondPass sp : fkSecondPassList ) {
				if ( sp.isInPrimaryKey() ) {
					final String referencedEntityName = sp.getReferencedEntityName();
					final PersistentClass classMapping = getEntityBinding( referencedEntityName );
					if ( classMapping == null ) {
						throw new HibernateException("Primary key referenced an unknown entity: "
													+ referencedEntityName );
					}
					final String dependentTable = classMapping.getTable().getQualifiedTableName().render();
					if ( !isADependencyOf.containsKey( dependentTable ) ) {
						isADependencyOf.put( dependentTable, new HashSet<>() );
					}
					isADependencyOf.get( dependentTable ).add( sp );
				}
				else {
					endOfQueueFkSecondPasses.add( sp );
				}
			}

			// using the isADependencyOf map we order the FkSecondPass recursively instances into the right order for processing
			final List<FkSecondPass> orderedFkSecondPasses = new ArrayList<>( fkSecondPassList.size() );
			for ( String tableName : isADependencyOf.keySet() ) {
				buildRecursiveOrderedFkSecondPasses( orderedFkSecondPasses, isADependencyOf, tableName, tableName );
			}

			// process the ordered FkSecondPasses
			for ( FkSecondPass sp : orderedFkSecondPasses ) {
				sp.doSecondPass( getEntityBindingMap() );
			}

			processSecondPasses( secondaryTableFromAnnotationSecondPassesList );

			processEndOfQueue( endOfQueueFkSecondPasses );

			fkSecondPassList.clear();
		}
	}

	/**
	 * Recursively builds a list of {@link FkSecondPass} instances ready to be processed in this order.
	 * Checking all dependencies recursively seems quite expensive, but the original code just relied
	 * on some sort of table name sorting which failed in certain circumstances.
	 * <p>
	 * See {@code ANN-722} and {@code ANN-730}
	 *
	 * @param orderedFkSecondPasses The list containing the {@link FkSecondPass} instances ready
	 * for processing.
	 * @param isADependencyOf Our lookup data structure to determine dependencies between tables
	 * @param startTable Table name to start recursive algorithm.
	 * @param currentTable The current table name used to check for 'new' dependencies.
	 */
	private void buildRecursiveOrderedFkSecondPasses(
			List<FkSecondPass> orderedFkSecondPasses,
			Map<String, Set<FkSecondPass>> isADependencyOf,
			String startTable,
			String currentTable) {
		final Set<FkSecondPass> dependencies = isADependencyOf.get( currentTable );
		if ( dependencies != null ) {
			for ( FkSecondPass pass : dependencies ) {
				final String dependentTable = pass.getValue().getTable().getQualifiedTableName().render();
				if ( dependentTable.compareTo( startTable ) != 0 ) {
					buildRecursiveOrderedFkSecondPasses( orderedFkSecondPasses, isADependencyOf, startTable, dependentTable );
				}
				if ( !orderedFkSecondPasses.contains( pass ) ) {
					orderedFkSecondPasses.add( 0, pass );
				}
			}
		}
		// else bottom out
	}

	private void processEndOfQueue(List<FkSecondPass> endOfQueueFkSecondPasses) {
		/*
		 * If a second pass raises a recoverableException, queue it for next round
		 * stop of no pass has to be processed or if the number of pass to processes
		 * does not diminish between two rounds.
		 * If some failing pass remain, raise the original exception
		 */
		boolean stopProcess = false;
		RuntimeException originalException = null;
		while ( !stopProcess ) {
			List<FkSecondPass> failingSecondPasses = new ArrayList<>();
			for ( FkSecondPass pass : endOfQueueFkSecondPasses ) {
				try {
					pass.doSecondPass( getEntityBindingMap() );
				}
				catch (FailedSecondPassException e) {
					failingSecondPasses.add( pass );
					if ( originalException == null ) {
						originalException = (RuntimeException) e.getCause();
					}
				}
			}
			stopProcess = failingSecondPasses.isEmpty()
							|| failingSecondPasses.size() == endOfQueueFkSecondPasses.size();
			endOfQueueFkSecondPasses = failingSecondPasses;
		}
		if ( !endOfQueueFkSecondPasses.isEmpty() ) {
			assert originalException != null;
			throw originalException;
		}
	}

	private void secondPassCompileForeignKeys(MetadataBuildingContext buildingContext) {
		int uniqueInteger = 0;
		final Set<ForeignKey> done = new HashSet<>();
		for ( Table table : collectTableMappings() ) {
			table.setUniqueInteger( uniqueInteger++ );
			secondPassCompileForeignKeys( table, done, buildingContext );
		}
	}

	protected void secondPassCompileForeignKeys(Table table, Set<ForeignKey> done, MetadataBuildingContext buildingContext)
			throws MappingException {
		table.createForeignKeys( buildingContext );

		final Dialect dialect = getDialect();
		for ( ForeignKey foreignKey : table.getForeignKeyCollection() ) {
			if ( !done.contains( foreignKey ) ) {
				done.add( foreignKey );
				final PersistentClass referencedClass = foreignKey.resolveReferencedClass(this);

				if ( referencedClass.isJoinedSubclass() ) {
					secondPassCompileForeignKeys( referencedClass.getSuperclass().getTable(), done, buildingContext);
				}

				// the ForeignKeys created in the first pass did not have their referenced table initialized
				if ( foreignKey.getReferencedTable() == null ) {
					foreignKey.setReferencedTable( referencedClass.getTable() );
				}

				final Identifier nameIdentifier =
						getMetadataBuildingOptions().getImplicitNamingStrategy()
								.determineForeignKeyName( new ForeignKeyNameSource( foreignKey, table, buildingContext ) );
				foreignKey.setName( nameIdentifier.render( dialect ) );

				foreignKey.alignColumns();
			}
		}
	}

	private void processPropertyReferences() {
		if ( delayedPropertyReferenceHandlers != null ) {
			log.debug( "Processing association property references" );

			for ( DelayedPropertyReferenceHandler delayedPropertyReferenceHandler : delayedPropertyReferenceHandlers ) {
				delayedPropertyReferenceHandler.process( this );
			}

			delayedPropertyReferenceHandlers.clear();
		}
	}

	private Map<String,NaturalIdUniqueKeyBinder> naturalIdUniqueKeyBinderMap;

	@Override
	public NaturalIdUniqueKeyBinder locateNaturalIdUniqueKeyBinder(String entityName) {
		return naturalIdUniqueKeyBinderMap == null ? null : naturalIdUniqueKeyBinderMap.get( entityName );
	}

	@Override
	public void registerNaturalIdUniqueKeyBinder(String entityName, NaturalIdUniqueKeyBinder ukBinder) {
		if ( naturalIdUniqueKeyBinderMap == null ) {
			naturalIdUniqueKeyBinderMap = new HashMap<>();
		}
		final NaturalIdUniqueKeyBinder previous = naturalIdUniqueKeyBinderMap.put( entityName, ukBinder );
		if ( previous != null ) {
			throw new AssertionFailure( "Previous NaturalIdUniqueKeyBinder already registered for entity name : " + entityName );
		}
	}

	private void processNaturalIdUniqueKeyBinders() {
		if ( naturalIdUniqueKeyBinderMap != null ) {
			for ( NaturalIdUniqueKeyBinder naturalIdUniqueKeyBinder : naturalIdUniqueKeyBinderMap.values() ) {
				naturalIdUniqueKeyBinder.process();
			}
			naturalIdUniqueKeyBinderMap.clear();
		}
	}

	private void processCachingOverrides() {
		if ( bootstrapContext.getCacheRegionDefinitions() != null ) {
			for ( CacheRegionDefinition cacheRegionDefinition : bootstrapContext.getCacheRegionDefinitions() ) {
				if ( cacheRegionDefinition.regionType() == CacheRegionDefinition.CacheRegionType.ENTITY ) {
					final PersistentClass entityBinding = getEntityBinding( cacheRegionDefinition.role() );
					if ( entityBinding == null ) {
						throw new HibernateException(
								"Cache override referenced an unknown entity : " + cacheRegionDefinition.role()
						);
					}
					if ( !(entityBinding instanceof RootClass rootClass) ) {
						throw new HibernateException(
								"Cache override referenced a non-root entity : " + cacheRegionDefinition.role()
						);
					}
					entityBinding.setCached( true );
					rootClass.setCacheRegionName( cacheRegionDefinition.region() );
					rootClass.setCacheConcurrencyStrategy( cacheRegionDefinition.usage() );
					rootClass.setLazyPropertiesCacheable( cacheRegionDefinition.cacheLazy() );
				}
				else if ( cacheRegionDefinition.regionType() == CacheRegionDefinition.CacheRegionType.COLLECTION ) {
					final Collection collectionBinding = getCollectionBinding( cacheRegionDefinition.role() );
					if ( collectionBinding == null ) {
						throw new HibernateException(
								"Cache override referenced an unknown collection role : " + cacheRegionDefinition.role()
						);
					}
					collectionBinding.setCacheRegionName( cacheRegionDefinition.region() );
					collectionBinding.setCacheConcurrencyStrategy( cacheRegionDefinition.usage() );
				}
			}
		}
	}

	@Override
	public boolean isInSecondPass() {
		return inSecondPass;
	}

	/**
	 * Builds the complete and immutable Metadata instance from the collected info.
	 *
	 * @return The complete and immutable Metadata instance
	 */
	public MetadataImpl buildMetadataInstance(MetadataBuildingContext buildingContext) {
		processSecondPasses( buildingContext );
		processExportableProducers();

		try {
			return new MetadataImpl(
					uuid,
					options,
					entityBindingMap,
					composites,
					genericComponentsMap,
					embeddableDiscriminatorTypesMap,
					mappedSuperClasses,
					collectionBindingMap,
					typeDefRegistry.copyRegistrationMap(),
					filterDefinitionMap,
					fetchProfileMap,
					imports,
					idGeneratorDefinitionMap,
					namedQueryMap,
					namedNativeQueryMap,
					namedProcedureCallMap,
					sqlResultSetMappingMap,
					namedEntityGraphMap,
					sqlFunctionMap,
					getDatabase(),
					bootstrapContext
			);
		}
		finally {
			getBootstrapContext().release();
		}
	}

	private void processExportableProducers() {
		// for now we only handle id generators as ExportableProducers

		final Dialect dialect = getDialect();

		for ( PersistentClass entityBinding : entityBindingMap.values() ) {
			entityBinding.assignCheckConstraintsToTable( dialect, bootstrapContext.getTypeConfiguration() );
			if ( entityBinding instanceof RootClass rootClass ) {
				handleIdentifierValueBinding(
						entityBinding.getIdentifier(),
						dialect,
						rootClass,
						entityBinding.getIdentifierProperty()
				);
			}
		}

		for ( Collection collection : collectionBindingMap.values() ) {
			if ( collection instanceof IdentifierCollection identifierCollection ) {
				handleIdentifierValueBinding(
						identifierCollection.getIdentifier(),
						dialect,
						null,
						null
				);
			}
		}
	}

	private void handleIdentifierValueBinding(
			KeyValue identifierValueBinding, Dialect dialect, RootClass entityBinding, Property identifierProperty) {
		try {
			identifierValueBinding.createGenerator( dialect, entityBinding, identifierProperty, this );
		}
		catch (MappingException e) {
			// Ignore this for now, the reasoning being "non-reflective" binding as needed
			// by tools. We want to hold off requiring classes being present until we
			// try to build the SF. Here, just building the Metadata, it is "ok" for an
			// exception to occur, the same exception will happen later as we build the SF.
			log.debugf( "Ignoring exception thrown when trying to build IdentifierGenerator as part of Metadata building", e );
		}
	}

	@Override
	public String getDefaultCatalog() {
		final String defaultCatalog = configurationService.getSetting( DEFAULT_CATALOG, StandardConverters.STRING );
		return defaultCatalog == null ? persistenceUnitMetadata.getDefaultCatalog() : defaultCatalog;
	}

	@Override
	public String getDefaultSchema() {
		final String defaultSchema = configurationService.getSetting( DEFAULT_SCHEMA, StandardConverters.STRING );
		return defaultSchema == null ? persistenceUnitMetadata.getDefaultSchema() : defaultSchema;
	}

	@Override
	public SqlStringGenerationContext getSqlStringGenerationContext() {
		return fromExplicit( database.getJdbcEnvironment(), database, getDefaultCatalog(), getDefaultSchema() );
	}
}
