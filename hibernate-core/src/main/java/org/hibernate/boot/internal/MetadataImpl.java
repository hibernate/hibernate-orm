/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.NamedHqlQueryDefinition;
import org.hibernate.boot.spi.NamedNativeQueryDefinition;
import org.hibernate.boot.spi.NamedProcedureCallDefinition;
import org.hibernate.boot.spi.NamedResultSetMappingDefinition;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.internal.NamedQueryRepositoryImpl;
import org.hibernate.query.named.NamedQueryRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;
import org.hibernate.type.spi.TypeConfiguration;

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

	private final MutableIdentifierGeneratorFactory identifierGeneratorFactory;

	private final Map<String,PersistentClass> entityBindingMap;
	private final Map<Class, MappedSuperclass> mappedSuperclassMap;
	private final Map<String,Collection> collectionBindingMap;
	private final Map<String, TypeDefinition> typeDefinitionMap;
	private final Map<String, FilterDefinition> filterDefinitionMap;
	private final Map<String, FetchProfile> fetchProfileMap;
	private final Map<String, String> imports;
	private final Map<String, IdentifierGeneratorDefinition> idGeneratorDefinitionMap;
	private final Map<String, NamedHqlQueryDefinition> namedQueryMap;
	private final Map<String, NamedNativeQueryDefinition> namedNativeQueryMap;
	private final Map<String, NamedProcedureCallDefinition> namedProcedureCallMap;
	private final Map<String, NamedResultSetMappingDefinition> sqlResultSetMappingMap;
	private final Map<String, NamedEntityGraphDefinition> namedEntityGraphMap;
	private final Map<String, SQLFunction> sqlFunctionMap;
	private final Database database;

	@SuppressWarnings("WeakerAccess")
	public MetadataImpl(
			UUID uuid,
			MetadataBuildingOptions metadataBuildingOptions,
			MutableIdentifierGeneratorFactory identifierGeneratorFactory,
			Map<String, PersistentClass> entityBindingMap,
			Map<Class, MappedSuperclass> mappedSuperclassMap,
			Map<String, Collection> collectionBindingMap,
			Map<String, TypeDefinition> typeDefinitionMap,
			Map<String, FilterDefinition> filterDefinitionMap,
			Map<String, FetchProfile> fetchProfileMap,
			Map<String, String> imports,
			Map<String, IdentifierGeneratorDefinition> idGeneratorDefinitionMap,
			Map<String, NamedHqlQueryDefinition> namedQueryMap,
			Map<String, NamedNativeQueryDefinition> namedNativeQueryMap,
			Map<String, NamedProcedureCallDefinition> namedProcedureCallMap,
			Map<String, NamedResultSetMappingDefinition> sqlResultSetMappingMap,
			Map<String, NamedEntityGraphDefinition> namedEntityGraphMap,
			Map<String, SQLFunction> sqlFunctionMap,
			Database database,
			BootstrapContext bootstrapContext) {
		this.uuid = uuid;
		this.metadataBuildingOptions = metadataBuildingOptions;
		this.identifierGeneratorFactory = identifierGeneratorFactory;
		this.entityBindingMap = entityBindingMap;
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

	/**
	 * Retrieve the {@link Type} resolver associated with this factory.
	 *
	 * @return The type resolver
	 *
	 * @deprecated (since 5.3) No replacement, access to and handling of Types will be much different in 6.0
	 */
	@Deprecated
	public TypeResolver getTypeResolver() {
		return bootstrapContext.getTypeConfiguration().getTypeResolver();
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		final SessionFactoryBuilderImpl defaultBuilder = new SessionFactoryBuilderImpl( this, bootstrapContext );

		final ClassLoaderService cls = metadataBuildingOptions.getServiceRegistry().getService( ClassLoaderService.class );
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
	public MutableIdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return identifierGeneratorFactory;
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
	public NamedHqlQueryDefinition getNamedHqlQueryMapping(String name) {
		return namedQueryMap.get( name );
	}

	@Override
	public void visitNamedHqlQueryDefinitions(Consumer<NamedHqlQueryDefinition> definitionConsumer) {
		namedQueryMap.values().forEach( definitionConsumer );
	}

	@Override
	public NamedNativeQueryDefinition getNamedNativeQueryMapping(String name) {
		return namedNativeQueryMap.get( name );
	}

	@Override
	public void visitNamedNativeQueryDefinitions(Consumer<NamedNativeQueryDefinition> definitionConsumer) {
		namedNativeQueryMap.values().forEach( definitionConsumer );
	}

	@Override
	public void visitNamedProcedureCallDefinition(Consumer<NamedProcedureCallDefinition> definitionConsumer) {
		namedProcedureCallMap.values().forEach( definitionConsumer );
	}

	@Override
	public NamedResultSetMappingDefinition getResultSetMapping(String name) {
		return sqlResultSetMappingMap.get( name );
	}

	@Override
	public void visitNamedResultSetMappingDefinition(Consumer<NamedResultSetMappingDefinition> definitionConsumer) {
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
	public Map<String, SQLFunction> getSqlFunctionMap() {
		return sqlFunctionMap;
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
	public NamedQueryRepository buildNamedQueryRepository(SessionFactoryImplementor sessionFactory) {
		return new NamedQueryRepositoryImpl(
				buildNamedHqlMementos( sessionFactory ),
				buildNamedNativeMementos( sessionFactory ),
				buildProcedureCallMementos( sessionFactory ),
				buildResultSetMappingMementos( sessionFactory )
		);
	}

	private Map<String, NamedHqlQueryMemento> buildNamedHqlMementos(SessionFactoryImplementor sessionFactory) {
		final HashMap<String, NamedHqlQueryMemento> map = new HashMap<>();
		if ( namedQueryMap != null ) {
			namedQueryMap.forEach( (key, value) -> map.put( key, value.resolve( sessionFactory ) ) );
		}
		return map;
	}

	private Map<String, NamedNativeQueryMemento> buildNamedNativeMementos(SessionFactoryImplementor sessionFactory) {
		final HashMap<String, NamedNativeQueryMemento> map = new HashMap<>();
		if ( namedNativeQueryMap != null ) {
			namedNativeQueryMap.forEach( (key, value) -> map.put( key, value.resolve( sessionFactory ) ) );
		}
		return map;
	}

	private Map<String, NamedCallableQueryMemento> buildProcedureCallMementos(SessionFactoryImplementor sessionFactory) {
		final Map<String, NamedCallableQueryMemento> map = new HashMap<>();
		if ( namedProcedureCallMap != null ) {
			namedProcedureCallMap.forEach( (key, value) -> map.put( key, value.resolve( sessionFactory ) ) );
		}
		return map;
	}

	private Map<String, NamedResultSetMappingMemento> buildResultSetMappingMementos(SessionFactoryImplementor sessionFactory) {
		final HashMap<String, NamedResultSetMappingMemento> map = new HashMap<>();
		if ( sqlResultSetMappingMap != null ) {
			sqlResultSetMappingMap.forEach( (key, value) -> map.put( key, value.resolve( sessionFactory ) ) );
		}
		return map;
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

	public Map<Class, MappedSuperclass> getMappedSuperclassMap() {
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

}
