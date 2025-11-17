/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategyLegacy;
import org.hibernate.boot.model.relational.Database;
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
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.query.internal.NamedObjectRepositoryImpl;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;
import org.hibernate.type.spi.TypeConfiguration;

import static java.lang.String.join;
import static java.util.Collections.emptySet;
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
		final var defaultBuilder = getFactoryBuilder();
		SessionFactoryBuilder builder = null;
		List<String> activeFactoryNames = null;
		for ( var discoveredBuilderFactory : getSessionFactoryBuilderFactories() ) {
			final SessionFactoryBuilder returnedBuilder =
					discoveredBuilderFactory.getSessionFactoryBuilder( this, defaultBuilder );
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
					"Multiple active SessionFactoryBuilderFactory definitions were discovered: " +
							join( ", ", activeFactoryNames )
			);
		}

		return builder == null ? defaultBuilder : builder;
	}

	private Iterable<SessionFactoryBuilderFactory> getSessionFactoryBuilderFactories() {
		return getClassLoaderService().loadJavaServices( SessionFactoryBuilderFactory.class );
	}

	private SessionFactoryBuilderImplementor getFactoryBuilder() {
		return metadataBuildingOptions.getServiceRegistry()
				.requireService( SessionFactoryBuilderService.class )
				.createSessionFactoryBuilder( this, bootstrapContext );
	}

	private ClassLoaderService getClassLoaderService() {
		return metadataBuildingOptions.getServiceRegistry().requireService( ClassLoaderService.class );
	}

	@Override
	public SessionFactoryImplementor buildSessionFactory() {
		return (SessionFactoryImplementor) getSessionFactoryBuilder().build();
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
		entityBindingMap.forEach( (s, persistentClass)
				-> contributors.add( persistentClass.getContributor() ) );
		for ( var namespace : database.getNamespaces() ) {
			for ( var table : namespace.getTables() ) {
				contributors.add( table.getContributor() );
			}
			for ( var sequence : namespace.getSequences() ) {
				contributors.add( sequence.getContributor() );
			}
		}
		return contributors;
	}

	@Override
	public java.util.Collection<Table> collectTableMappings() {
		final ArrayList<Table> tables = new ArrayList<>();
		for ( var namespace : database.getNamespaces() ) {
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
		final var columnOrderingStrategy = metadataBuildingOptions.getColumnOrderingStrategy();
		// No need to order columns when using the no-op strategy
		if ( columnOrderingStrategy != ColumnOrderingStrategyLegacy.INSTANCE ) {
			final boolean shouldOrderTableColumns = forceOrdering || shouldOrderTableColumns();
			for ( var namespace : database.getNamespaces() ) {
				if ( shouldOrderTableColumns ) {
					for ( var table : namespace.getTables() ) {
						handleTable( table, columnOrderingStrategy );
						handlePrimaryKey( table, columnOrderingStrategy );
						handleForeignKeys( table, columnOrderingStrategy );
					}
				}
				for ( var userDefinedType : namespace.getUserDefinedTypes() ) {
					handleUDT( userDefinedType, columnOrderingStrategy );
				}
			}
		}
	}

	private void handleTable(Table table, ColumnOrderingStrategy columnOrderingStrategy) {
		final var tableColumns = columnOrderingStrategy.orderTableColumns( table, this );
		if ( tableColumns != null ) {
			table.reorderColumns( tableColumns );
		}
	}

	private void handleUDT(UserDefinedType userDefinedType, ColumnOrderingStrategy columnOrderingStrategy) {
		if ( userDefinedType instanceof UserDefinedObjectType objectType
				&& objectType.getColumns().size() > 1 ) {
			final var objectTypeColumns =
					columnOrderingStrategy.orderUserDefinedTypeColumns( objectType, this );
			if ( objectTypeColumns != null ) {
				objectType.reorderColumns( objectTypeColumns );
			}
		}
	}

	private void handleForeignKeys(Table table, ColumnOrderingStrategy columnOrderingStrategy) {
		for ( var foreignKey : table.getForeignKeyCollection() ) {
			final var columns = foreignKey.getColumns();
			if ( columns.size() > 1 ) {
				if ( foreignKey.getReferencedColumns().isEmpty() ) {
					final var targetPrimaryKey =
							foreignKey.getReferencedTable().getPrimaryKey();
					// Make sure we order the columns of the primary key first,
					// so that foreign key ordering can rely on this
					if ( targetPrimaryKey.getOriginalOrder() == null ) {
						final var primaryKeyColumns =
								columnOrderingStrategy.orderConstraintColumns( targetPrimaryKey, this );
						if ( primaryKeyColumns != null ) {
							targetPrimaryKey.reorderColumns( primaryKeyColumns );
						}
					}

					// Patch up the order of foreign keys based on new order of the target primary key
					final int[] originalPrimaryKeyOrder = targetPrimaryKey.getOriginalOrder();
					if ( originalPrimaryKeyOrder != null ) {
						final var foreignKeyColumnsCopy = new ArrayList<>( columns );
						for ( int i = 0; i < foreignKeyColumnsCopy.size(); i++ ) {
							columns.set( i, foreignKeyColumnsCopy.get( originalPrimaryKeyOrder[i] ) );
						}
					}
				}
			}
		}
	}

	private void handlePrimaryKey(Table table, ColumnOrderingStrategy columnOrderingStrategy) {
		final var primaryKey = table.getPrimaryKey();
		if ( primaryKey != null
				&& primaryKey.getColumns().size() > 1
				&& primaryKey.getOriginalOrder() == null ) {
			final var primaryKeyColumns =
					columnOrderingStrategy.orderConstraintColumns( primaryKey, this );
			if ( primaryKeyColumns != null ) {
				primaryKey.reorderColumns( primaryKeyColumns );
			}
		}
	}

	private boolean shouldOrderTableColumns() {
		final var settings =
				metadataBuildingOptions.getServiceRegistry()
						.requireService( ConfigurationService.class )
						.getSettings();
		for ( var grouping : ActionGrouping.interpret( this, settings ) ) {
			if ( isColumnOrderingRelevant( grouping.scriptAction() )
				|| isColumnOrderingRelevant( grouping.databaseAction() ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean isColumnOrderingRelevant(Action grouping) {
		return switch ( grouping ) {
			case CREATE, CREATE_DROP, CREATE_ONLY -> true;
			default -> false;
		};
	}

	@Override
	public void validate() throws MappingException {
		for ( var entityBinding : this.getEntityBindings() ) {
			entityBinding.validate( this );
		}

		for ( var collectionBinding : this.getCollectionBindings() ) {
			collectionBinding.validate( this );
		}
	}

	@Override
	public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
		return mappedSuperclassMap == null
				? emptySet()
				: new HashSet<>( mappedSuperclassMap.values() );
	}

	@Override
	public void initSessionFactory(SessionFactoryImplementor sessionFactory) {
		// must not use BootstrapContext services here
		final var registry = sessionFactory.getServiceRegistry();
		assert registry != null;
		final var configurationService = registry.requireService( ConfigurationService.class );
		final var classLoaderService = registry.requireService( ClassLoaderService.class );
		final var eventListenerRegistry = sessionFactory.getEventListenerRegistry();
		configurationService.getSettings().forEach( (propertyName, value) -> {
			if ( propertyName.startsWith( EVENT_LISTENER_PREFIX ) ) {
				final String eventTypeName = propertyName.substring( EVENT_LISTENER_PREFIX.length() + 1 );
				final var eventType = EventType.resolveEventTypeByName( eventTypeName );
				final String listeners = (String) value;
				appendListeners( eventListenerRegistry, classLoaderService, listeners, eventType );
			}
		} );
	}

	private <T> void appendListeners(
			EventListenerRegistry eventListenerRegistry,
			ClassLoaderService classLoaderService,
			String listeners,
			EventType<T> eventType) {
		final var eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
		for ( String listenerImpl : splitAtCommas( listeners ) ) {
			@SuppressWarnings("unchecked")
			T listener = (T) instantiate( listenerImpl, classLoaderService );
			if ( !eventType.baseListenerInterface().isInstance( listener ) ) {
				throw new HibernateException( "Event listener '" + listenerImpl
						+ "' must implement '" + eventType.baseListenerInterface().getName() + "'");
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
		final var persistentClass = entityBindingMap.get( entityName );
		if ( persistentClass == null ) {
			throw new MappingException( "Persistent class not known: " + entityName );
		}
		return persistentClass.getIdentifier().getType();
	}

	@Override
	public String getIdentifierPropertyName(String entityName) throws MappingException {
		final var persistentClass = entityBindingMap.get( entityName );
		if ( persistentClass == null ) {
			throw new MappingException( "Persistent class not known: " + entityName );
		}
		if ( !persistentClass.hasIdentifierProperty() ) {
			return null;
		}
		return persistentClass.getIdentifierProperty().getName();
	}

	@Override
	public org.hibernate.type.Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
		final var persistentClass = entityBindingMap.get( entityName );
		if ( persistentClass == null ) {
			throw new MappingException( "Persistent class not known: " + entityName );
		}
		final var referencedProperty = persistentClass.getReferencedProperty( propertyName );
		if ( referencedProperty == null ) {
			throw new MappingException( "Property not known: " + entityName + '.' + propertyName );
		}
		return referencedProperty.getType();
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
}
