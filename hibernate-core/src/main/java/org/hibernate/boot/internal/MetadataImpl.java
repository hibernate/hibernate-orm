/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategyLegacy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.query.internal.NamedObjectRepositoryImpl;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.cfg.AvailableSettings.EVENT_LISTENER_PREFIX;
import static org.hibernate.internal.util.StringHelper.splitAtCommas;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * Container for configuration data collected during binding the metamodel.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class MetadataImpl implements MetadataImplementor, Serializable {

	private final UUID uuid;
	private final MetadataBuildingOptions metadataBuildingOptions;
	private final BootstrapContext bootstrapContext;

	private final Map<String,PersistentClass> entityBindingMap;
	private final List<Component> composites;
	private final Map<Class<?>, Component> genericComponentsMap;
	private final Map<Class<?>, DiscriminatorType<?>> embeddableDiscriminatorTypesMap;
	private final Map<Class<?>, MappedSuperclass> mappedSuperclassMap;
	private final Map<String,Collection> collectionBindingMap;
	private final Map<String, TypeDefinition> typeDefinitionMap;
	private final Map<String, FilterDefinition> filterDefinitionMap;
	private final Map<String, FetchProfile> fetchProfileMap;
	private final Map<String, String> imports;
	private final Map<String, IdentifierGeneratorDefinition> idGeneratorDefinitionMap;
	private final Map<String, NamedHqlQueryDefinition<?>> namedQueryMap;
	private final Map<String, NamedNativeQueryDefinition<?>> namedNativeQueryMap;
	private final Map<String, NamedProcedureCallDefinition> namedProcedureCallMap;
	private final Map<String, NamedResultSetMappingDescriptor> sqlResultSetMappingMap;
	private final Map<String, NamedEntityGraphDefinition> namedEntityGraphMap;
	private final Map<String, SqmFunctionDescriptor> sqlFunctionMap;
	private final Database database;

	public MetadataImpl(
			UUID uuid,
			MetadataBuildingOptions metadataBuildingOptions,
			Map<String, PersistentClass> entityBindingMap,
			List<Component> composites,
			Map<Class<?>, Component> genericComponentsMap,
			Map<Class<?>, DiscriminatorType<?>> embeddableDiscriminatorTypesMap,
			Map<Class<?>, MappedSuperclass> mappedSuperclassMap,
			Map<String, Collection> collectionBindingMap,
			Map<String, TypeDefinition> typeDefinitionMap,
			Map<String, FilterDefinition> filterDefinitionMap,
			Map<String, FetchProfile> fetchProfileMap,
			Map<String, String> imports,
			Map<String, IdentifierGeneratorDefinition> idGeneratorDefinitionMap,
			Map<String, NamedHqlQueryDefinition<?>> namedQueryMap,
			Map<String, NamedNativeQueryDefinition<?>> namedNativeQueryMap,
			Map<String, NamedProcedureCallDefinition> namedProcedureCallMap,
			Map<String, NamedResultSetMappingDescriptor> sqlResultSetMappingMap,
			Map<String, NamedEntityGraphDefinition> namedEntityGraphMap,
			Map<String, SqmFunctionDescriptor> sqlFunctionMap,
			Database database,
			BootstrapContext bootstrapContext) {
		this.uuid = uuid;
		this.metadataBuildingOptions = metadataBuildingOptions;
		this.entityBindingMap = entityBindingMap;
		this.composites = composites;
		this.genericComponentsMap = genericComponentsMap;
		this.embeddableDiscriminatorTypesMap = embeddableDiscriminatorTypesMap;
		this.mappedSuperclassMap = mappedSuperclassMap;
		this.collectionBindingMap = collectionBindingMap;
		this.typeDefinitionMap = typeDefinitionMap;
		this.filterDefinitionMap = filterDefinitionMap;
		this.fetchProfileMap = fetchProfileMap;
		this.imports = imports;
		this.idGeneratorDefinitionMap = idGeneratorDefinitionMap;
		this.namedQueryMap = namedQueryMap;
		this.namedNativeQueryMap = namedNativeQueryMap;
		this.namedProcedureCallMap = namedProcedureCallMap;
		this.sqlResultSetMappingMap = sqlResultSetMappingMap;
		this.namedEntityGraphMap = namedEntityGraphMap;
		this.sqlFunctionMap = sqlFunctionMap;
		this.database = database;
		this.bootstrapContext = bootstrapContext;
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return metadataBuildingOptions;
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
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		final SessionFactoryBuilderService factoryBuilderService = metadataBuildingOptions.getServiceRegistry().requireService( SessionFactoryBuilderService.class );
		final SessionFactoryBuilderImplementor defaultBuilder = factoryBuilderService.createSessionFactoryBuilder( this, bootstrapContext );

		final ClassLoaderService cls = metadataBuildingOptions.getServiceRegistry().requireService( ClassLoaderService.class );
		final java.util.Collection<SessionFactoryBuilderFactory> discoveredBuilderFactories = cls.loadJavaServices( SessionFactoryBuilderFactory.class );

		SessionFactoryBuilder builder = null;
		List<String> activeFactoryNames = null;

		for ( SessionFactoryBuilderFactory discoveredBuilderFactory : discoveredBuilderFactories ) {
			final SessionFactoryBuilder returnedBuilder = discoveredBuilderFactory.getSessionFactoryBuilder( this, defaultBuilder );
			if ( returnedBuilder != null ) {
				if ( activeFactoryNames == null ) {
					activeFactoryNames = new ArrayList<>();
				}
				activeFactoryNames.add( discoveredBuilderFactory.getClass().getName() );
				builder = returnedBuilder;
			}
		}

		if ( activeFactoryNames != null && activeFactoryNames.size() > 1 ) {
			throw new HibernateException(
					"Multiple active SessionFactoryBuilderFactory definitions were discovered : " +
							String.join(", ", activeFactoryNames)
			);
		}

		if ( builder != null ) {
			return builder;
		}

		return defaultBuilder;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return getSessionFactoryBuilder().build();
	}

	@Override
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public java.util.Collection<PersistentClass> getEntityBindings() {
		return entityBindingMap.values();
	}

	@Override
	public PersistentClass getEntityBinding(String entityName) {
		return entityBindingMap.get( entityName );
	}

	@Override
	public java.util.Collection<Collection> getCollectionBindings() {
		return collectionBindingMap.values();
	}

	@Override
	public Collection getCollectionBinding(String role) {
		return collectionBindingMap.get( role );
	}

	@Override
	public Map<String, String> getImports() {
		return imports;
	}

	@Override
	public NamedHqlQueryDefinition<?> getNamedHqlQueryMapping(String name) {
		return namedQueryMap.get( name );
	}

	@Override
	public void visitNamedHqlQueryDefinitions(Consumer<NamedHqlQueryDefinition<?>> definitionConsumer) {
		namedQueryMap.values().forEach( definitionConsumer );
	}

	@Override
	public NamedNativeQueryDefinition<?> getNamedNativeQueryMapping(String name) {
		return namedNativeQueryMap.get( name );
	}

	@Override
	public void visitNamedNativeQueryDefinitions(Consumer<NamedNativeQueryDefinition<?>> definitionConsumer) {
		namedNativeQueryMap.values().forEach( definitionConsumer );
	}

	@Override
	public NamedProcedureCallDefinition getNamedProcedureCallMapping(String name) {
		return namedProcedureCallMap.get( name );
	}

	@Override
	public void visitNamedProcedureCallDefinition(Consumer<NamedProcedureCallDefinition> definitionConsumer) {
		namedProcedureCallMap.values().forEach( definitionConsumer );
	}

	@Override
	public NamedResultSetMappingDescriptor getResultSetMapping(String name) {
		return sqlResultSetMappingMap.get( name );
	}

	@Override
	public void visitNamedResultSetMappingDefinition(Consumer<NamedResultSetMappingDescriptor> definitionConsumer) {
		sqlResultSetMappingMap.values().forEach( definitionConsumer );
	}

	@Override
	public TypeDefinition getTypeDefinition(String typeName) {
		return typeDefinitionMap.get( typeName );
	}

	@Override
	public Map<String, FilterDefinition> getFilterDefinitions() {
		return filterDefinitionMap;
	}

	@Override
	public FilterDefinition getFilterDefinition(String name) {
		return filterDefinitionMap.get( name );
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return fetchProfileMap.get( name );
	}

	@Override
	public java.util.Collection<FetchProfile> getFetchProfiles() {
		return fetchProfileMap.values();
	}

	@Override
	public NamedEntityGraphDefinition getNamedEntityGraph(String name) {
		return namedEntityGraphMap.get( name );
	}

	@Override
	public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs() {
		return namedEntityGraphMap;
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGenerator(String name) {
		return idGeneratorDefinitionMap.get( name );
	}

	@Override
	public Map<String, SqmFunctionDescriptor> getSqlFunctionMap() {
		return sqlFunctionMap;
	}

	@Override
	public Set<String> getContributors() {
		final HashSet<String> contributors = new HashSet<>();

		entityBindingMap.forEach(
				(s, persistentClass) -> contributors.add( persistentClass.getContributor() )
		);

		for ( Namespace namespace : database.getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				contributors.add( table.getContributor() );
			}

			for ( Sequence sequence : namespace.getSequences() ) {
				contributors.add( sequence.getContributor() );
			}
		}

		return contributors;
	}

	@Override
	public java.util.Collection<Table> collectTableMappings() {
		ArrayList<Table> tables = new ArrayList<>();
		for ( Namespace namespace : database.getNamespaces() ) {
			tables.addAll( namespace.getTables() );
		}
		return tables;
	}

	@Override
	public NamedObjectRepository buildNamedQueryRepository() {
		return new NamedObjectRepositoryImpl(
				mapOfSize( namedQueryMap.size() ),
				mapOfSize( namedNativeQueryMap.size() ),
				mapOfSize( namedProcedureCallMap.size() ),
				mapOfSize( sqlResultSetMappingMap.size() )
		);
	}

	@Override
	public void orderColumns(boolean forceOrdering) {
		final ColumnOrderingStrategy columnOrderingStrategy = metadataBuildingOptions.getColumnOrderingStrategy();
		if ( columnOrderingStrategy == ColumnOrderingStrategyLegacy.INSTANCE ) {
			// No need to order columns when using the no-op strategy
			return;
		}

		final boolean shouldOrderTableColumns = forceOrdering || shouldOrderTableColumns();

		for ( Namespace namespace : database.getNamespaces() ) {
			if ( shouldOrderTableColumns ) {
				for ( Table table : namespace.getTables() ) {
					final List<Column> tableColumns = columnOrderingStrategy.orderTableColumns( table, this );
					if ( tableColumns != null ) {
						table.reorderColumns( tableColumns );
					}
					final PrimaryKey primaryKey = table.getPrimaryKey();
					if ( primaryKey != null && primaryKey.getColumns()
							.size() > 1 && primaryKey.getOriginalOrder() == null ) {
						final List<Column> primaryKeyColumns = columnOrderingStrategy.orderConstraintColumns(
								primaryKey,
								this
						);
						if ( primaryKeyColumns != null ) {
							primaryKey.reorderColumns( primaryKeyColumns );
						}
					}
					for ( ForeignKey foreignKey : table.getForeignKeys().values() ) {
						final List<Column> columns = foreignKey.getColumns();
						if ( columns.size() > 1 ) {
							if ( foreignKey.getReferencedColumns().isEmpty() ) {
								final PrimaryKey foreignKeyTargetPrimaryKey = foreignKey.getReferencedTable()
										.getPrimaryKey();
								// Make sure we order the columns of the primary key first,
								// so that foreign key ordering can rely on this
								if ( foreignKeyTargetPrimaryKey.getOriginalOrder() == null ) {
									final List<Column> primaryKeyColumns = columnOrderingStrategy.orderConstraintColumns(
											foreignKeyTargetPrimaryKey,
											this
									);
									if ( primaryKeyColumns != null ) {
										foreignKeyTargetPrimaryKey.reorderColumns( primaryKeyColumns );
									}
								}

								// Patch up the order of foreign keys based on new order of the target primary key
								final int[] originalPrimaryKeyOrder = foreignKeyTargetPrimaryKey.getOriginalOrder();
								if ( originalPrimaryKeyOrder != null ) {
									final ArrayList<Column> foreignKeyColumnsCopy = new ArrayList<>( columns );
									for ( int i = 0; i < foreignKeyColumnsCopy.size(); i++ ) {
										columns.set( i, foreignKeyColumnsCopy.get( originalPrimaryKeyOrder[i] ) );
									}
								}
							}
						}
					}
				}
			}
			for ( UserDefinedType userDefinedType : namespace.getUserDefinedTypes() ) {
				if ( userDefinedType instanceof UserDefinedObjectType ) {
					final UserDefinedObjectType objectType = (UserDefinedObjectType) userDefinedType;
					if ( objectType.getColumns().size() > 1 ) {
						final List<Column> objectTypeColumns = columnOrderingStrategy.orderUserDefinedTypeColumns(
								objectType,
								this
						);
						if ( objectTypeColumns != null ) {
							objectType.reorderColumns( objectTypeColumns );
						}
					}
				}
			}
		}
	}

	private boolean shouldOrderTableColumns() {
		final ConfigurationService configurationService = metadataBuildingOptions.getServiceRegistry()
				.requireService( ConfigurationService.class );
		final Set<SchemaManagementToolCoordinator.ActionGrouping> groupings = SchemaManagementToolCoordinator.ActionGrouping.interpret(
				this,
				configurationService.getSettings()
		);
		if ( groupings.isEmpty() ) {
			return false;
		}
		for ( SchemaManagementToolCoordinator.ActionGrouping grouping : groupings ) {
			if ( isColumnOrderingRelevant( grouping.getScriptAction() ) || isColumnOrderingRelevant( grouping.getDatabaseAction() ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean isColumnOrderingRelevant(Action grouping) {
		switch ( grouping ) {
			case CREATE:
			case CREATE_DROP:
			case CREATE_ONLY:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void validate() throws MappingException {
		for ( PersistentClass entityBinding : this.getEntityBindings() ) {
			entityBinding.validate( this );
		}

		for ( Collection collectionBinding : this.getCollectionBindings() ) {
			collectionBinding.validate( this );
		}
	}

	@Override
	public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
		return mappedSuperclassMap == null
				? Collections.emptySet()
				: new HashSet<>( mappedSuperclassMap.values() );
	}

	@Override
	public void initSessionFactory(SessionFactoryImplementor sessionFactory) {
		final ServiceRegistryImplementor sessionFactoryServiceRegistry = sessionFactory.getServiceRegistry();

		assert sessionFactoryServiceRegistry != null;

		final EventListenerRegistry eventListenerRegistry = sessionFactoryServiceRegistry.requireService( EventListenerRegistry.class );
		final ConfigurationService cfgService = sessionFactoryServiceRegistry.requireService( ConfigurationService.class );
		final ClassLoaderService classLoaderService = sessionFactoryServiceRegistry.requireService( ClassLoaderService.class );

		for ( Map.Entry<String,Object> entry : cfgService.getSettings().entrySet() ) {
			final String propertyName = entry.getKey();
			if ( propertyName.startsWith( EVENT_LISTENER_PREFIX ) ) {
				final String eventTypeName = propertyName.substring( EVENT_LISTENER_PREFIX.length() + 1 );
				final EventType<?> eventType = EventType.resolveEventTypeByName( eventTypeName );
				final String listeners = (String) entry.getValue();
				appendListeners( eventListenerRegistry, classLoaderService, listeners, eventType );
			}
		}
	}

	private <T> void appendListeners(
			EventListenerRegistry eventListenerRegistry,
			ClassLoaderService classLoaderService,
			String listeners,
			EventType<T> eventType) {
		final EventListenerGroup<T> eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
		for ( String listenerImpl : splitAtCommas( listeners ) ) {
			@SuppressWarnings("unchecked")
			T listener = (T) instantiate( listenerImpl, classLoaderService );
			if ( !eventType.baseListenerInterface().isInstance( listener ) ) {
				throw new HibernateException( "Event listener '" + listenerImpl  + "' must implement '"
						+ eventType.baseListenerInterface().getName() + "'");
			}
			eventListenerGroup.appendListener( listener );
		}
	}

	private static Object instantiate(String listenerImpl, ClassLoaderService classLoaderService) {
		try {
			return classLoaderService.classForName( listenerImpl ).newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate event listener '" + listenerImpl + "'", e );
		}
	}

	@Override
	public void visitRegisteredComponents(Consumer<Component> consumer) {
		composites.forEach( consumer );
	}

	@Override
	public Component getGenericComponent(Class<?> componentClass) {
		return genericComponentsMap.get( componentClass );
	}

	@Override
	public DiscriminatorType<?> resolveEmbeddableDiscriminatorType(
			Class<?> embeddableClass,
			Supplier<DiscriminatorType<?>> supplier) {
		return embeddableDiscriminatorTypesMap.computeIfAbsent( embeddableClass, k -> supplier.get() );
	}

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
		final PersistentClass pc = entityBindingMap.get( entityName );
		if ( pc == null ) {
			throw new MappingException( "persistent class not known: " + entityName );
		}
		if ( !pc.hasIdentifierProperty() ) {
			return null;
		}
		return pc.getIdentifierProperty().getName();
	}

	@Override
	public org.hibernate.type.Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
		final PersistentClass pc = entityBindingMap.get( entityName );
		if ( pc == null ) {
			throw new MappingException( "persistent class not known: " + entityName );
		}
		Property prop = pc.getReferencedProperty( propertyName );
		if ( prop == null ) {
			throw new MappingException(
					"property not known: " +
							entityName + '.' + propertyName
			);
		}
		return prop.getType();
	}

	//Specific for copies only:

	public Map<String,PersistentClass> getEntityBindingMap() {
		return entityBindingMap;
	}

	public Map<String, Collection> getCollectionBindingMap() {
		return collectionBindingMap;
	}

	public Map<String, TypeDefinition> getTypeDefinitionMap() {
		return typeDefinitionMap;
	}

	public Map<String, FetchProfile> getFetchProfileMap() {
		return fetchProfileMap;
	}

	public Map<Class<?>, MappedSuperclass> getMappedSuperclassMap() {
		return mappedSuperclassMap;
	}

	public Map<String, IdentifierGeneratorDefinition> getIdGeneratorDefinitionMap() {
		return idGeneratorDefinitionMap;
	}

	public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphMap() {
		return namedEntityGraphMap;
	}

	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	public Map<String, NamedHqlQueryDefinition<?>> getNamedQueryMap() {
		return namedQueryMap;
	}

	public Map<String, NamedNativeQueryDefinition<?>> getNamedNativeQueryMap() {
		return namedNativeQueryMap;
	}

	public Map<String, NamedProcedureCallDefinition> getNamedProcedureCallMap() {
		return namedProcedureCallMap;
	}

	public Map<String, NamedResultSetMappingDescriptor> getSqlResultSetMappingMap() {
		return sqlResultSetMappingMap;
	}

	public java.util.List<org.hibernate.mapping.Component> getComposites() {
		return composites;
	}

	public Map<Class<?>, Component> getGenericComponentsMap() {
		return genericComponentsMap;
	}

	public Map<Class<?>, DiscriminatorType<?>> getEmbeddableDiscriminatorTypesMap() {
		return embeddableDiscriminatorTypesMap;
	}

	@Internal
	// called by the Quarkus extension
	public MetadataImpl trim() {
		return new MetadataImpl(
				getUUID(),
				getMetadataBuildingOptions(), //TODO Replace this
				getEntityBindingMap(),
				getComposites(),
				getGenericComponentsMap(),
				getEmbeddableDiscriminatorTypesMap(),
				getMappedSuperclassMap(),
				getCollectionBindingMap(),
				getTypeDefinitionMap(),
				getFilterDefinitions(),
				getFetchProfileMap(),
				getImports(), // ok
				getIdGeneratorDefinitionMap(),
				getNamedQueryMap(),
				getNamedNativeQueryMap(), // TODO might contain references to org.hibernate.loader.custom.ConstructorResultColumnProcessor, org.hibernate.type.TypeStandardSQLFunction
				getNamedProcedureCallMap(),
				getSqlResultSetMappingMap(), //TODO might contain NativeSQLQueryReturn (as namedNativeQueryMap above)
				getNamedEntityGraphs(), //TODO reference to *annotation* instance ! FIXME or ignore feature?
				getSqlFunctionMap(), // ok
				getDatabase(), // Cleaned up: used to include references to MetadataBuildingOptions, etc.
				getBootstrapContext() //FIXME WHOA!
		);
	}
}
