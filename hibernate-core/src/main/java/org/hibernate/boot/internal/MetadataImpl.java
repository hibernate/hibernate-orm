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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.spi.EntityMappingImplementor;
import org.hibernate.boot.model.query.spi.NamedHqlQueryDefinition;
import org.hibernate.boot.model.query.spi.NamedNativeQueryDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.cfg.annotations.NamedProcedureCallDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.spi.AuditMetadataBuilderImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.query.internal.NamedQueryRepositoryImpl;
import org.hibernate.query.named.spi.NamedCallableQueryMemento;
import org.hibernate.query.named.spi.NamedHqlQueryMemento;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.query.spi.ResultSetMappingDescriptor;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
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


	private final Map<String, EntityMappingHierarchy> entityMappingHierarchies;
	private final Map<String,PersistentClass> entityBindingMap;
	private final Map<Class, MappedSuperclass> mappedSuperclassMap;
	private final Map<String,Collection> collectionBindingMap;
	private final Map<String, FilterDefinition> filterDefinitionMap;
	private final Map<String, FetchProfile> fetchProfileMap;
	private final Map<String, String> imports;
	private final Map<String, IdentifierGeneratorDefinition> idGeneratorDefinitionMap;
	private final Map<String, NamedHqlQueryDefinition> namedHqlQueryMap;
	private final Map<String, NamedNativeQueryDefinition> namedNativeQueryMap;
	private final Map<String, NamedProcedureCallDefinition> namedProcedureCallMap;
	private final Map<String, ResultSetMappingDefinition> sqlResultSetMappingMap;
	private final Map<String, NamedEntityGraphDefinition> namedEntityGraphMap;
	private final Map<String, SqmFunctionTemplate> sqlFunctionMap;
	private final Database database;
	private final AuditMetadataBuilderImplementor auditMetadataBuilder;

	MetadataImpl(
			UUID uuid,
			MetadataBuildingOptions metadataBuildingOptions,
			Map<String, EntityMappingHierarchy> entityMappingHierarchies,
			Map<String, PersistentClass> entityBindingMap,
			Map<Class, MappedSuperclass> mappedSuperclassMap,
			Map<String, Collection> collectionBindingMap,
			Map<String, FilterDefinition> filterDefinitionMap,
			Map<String, FetchProfile> fetchProfileMap,
			Map<String, String> imports,
			Map<String, IdentifierGeneratorDefinition> idGeneratorDefinitionMap,
			Map<String, NamedHqlQueryDefinition> namedHqlQueryMap,
			Map<String, NamedNativeQueryDefinition> namedNativeQueryMap,
			Map<String, NamedProcedureCallDefinition> namedProcedureCallMap,
			Map<String, ResultSetMappingDefinition> sqlResultSetMappingMap,
			Map<String, NamedEntityGraphDefinition> namedEntityGraphMap,
			Map<String, SqmFunctionTemplate> sqlFunctionMap,
			AuditMetadataBuilderImplementor auditMetadataBuilder,
			Database database,
			BootstrapContext bootstrapContext) {
		this.uuid = uuid;
		this.metadataBuildingOptions = metadataBuildingOptions;
		this.entityMappingHierarchies = entityMappingHierarchies;
		this.entityBindingMap = entityBindingMap;
		this.mappedSuperclassMap = mappedSuperclassMap;
		this.collectionBindingMap = collectionBindingMap;
		this.filterDefinitionMap = filterDefinitionMap;
		this.fetchProfileMap = fetchProfileMap;
		this.imports = imports;
		this.idGeneratorDefinitionMap = idGeneratorDefinitionMap;
		this.namedHqlQueryMap = namedHqlQueryMap;
		this.namedNativeQueryMap = namedNativeQueryMap;
		this.namedProcedureCallMap = namedProcedureCallMap;
		this.sqlResultSetMappingMap = sqlResultSetMappingMap;
		this.namedEntityGraphMap = namedEntityGraphMap;
		this.sqlFunctionMap = sqlFunctionMap;
		this.database = database;
		this.auditMetadataBuilder = auditMetadataBuilder;
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
							String.join( ", ", activeFactoryNames )
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
	public java.util.Collection<EntityMappingHierarchy> getEntityHierarchies() {
		return entityMappingHierarchies.values();
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
	public NamedHqlQueryDefinition getNamedHqlQueryDefinition(String name) {
		return namedHqlQueryMap.get( name );
	}

	@Override
	public java.util.Collection<NamedHqlQueryDefinition> getNamedHqlQueryDefinitions() {
		return namedHqlQueryMap.values();
	}

	@Override
	public NamedNativeQueryDefinition getNamedNativeQueryDefinition(String name) {
		return namedNativeQueryMap.get( name );
	}

	@Override
	public java.util.Collection<NamedNativeQueryDefinition> getNamedNativeQueryDefinitions() {
		return namedNativeQueryMap.values();
	}

	@Override
	public java.util.Collection<NamedProcedureCallDefinition> getNamedProcedureCallDefinitions() {
		return namedProcedureCallMap.values();
	}

	@Override
	public ResultSetMappingDefinition getResultSetMapping(String name) {
		return sqlResultSetMappingMap.get( name );
	}

	@Override
	public Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions() {
		return sqlResultSetMappingMap;
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
	public Map<String, SqmFunctionTemplate> getSqlFunctionMap() {
		return sqlFunctionMap;
	}

	@Override
	public java.util.Collection<Table> collectTableMappings() {
		return collectMappedTableMappings().stream().map( t -> (Table) t ).collect( Collectors.toList() );
	}

	@Override
	public java.util.Collection<MappedTable> collectMappedTableMappings() {
		ArrayList<MappedTable> tables = new ArrayList<>();
		for ( MappedNamespace namespace : database.getNamespaces() ) {
			tables.addAll( namespace.getTables() );
		}
		return tables;
	}

	@Override
	public NamedQueryRepository buildNamedQueryRepository(SessionFactoryImplementor sessionFactory) {
		return new NamedQueryRepositoryImpl(
				buildNamedHqlDescriptorMap( sessionFactory ),
				buildNamedNativeDescriptorMap( sessionFactory ),
				buildProcedureCallMementos( sessionFactory ),
				buildSqlResultSetMappings( sessionFactory )
		);

	}

	private Map<String, NamedHqlQueryMemento> buildNamedHqlDescriptorMap(SessionFactoryImplementor sessionFactory) {
		if ( namedHqlQueryMap == null || namedHqlQueryMap.isEmpty() ) {
			return Collections.emptyMap();
		}

		final ConcurrentHashMap<String,NamedHqlQueryMemento> descriptorMap = new ConcurrentHashMap<>();

		for ( Map.Entry<String, NamedHqlQueryDefinition> entry : namedHqlQueryMap.entrySet() ) {
			descriptorMap.put(
					entry.getKey(),
					entry.getValue().resolve( sessionFactory )
			);
		}

		return descriptorMap;
	}

	private Map<String, NamedNativeQueryMemento> buildNamedNativeDescriptorMap(SessionFactoryImplementor sessionFactory) {
		if ( namedNativeQueryMap == null || namedNativeQueryMap.isEmpty() ) {
			return Collections.emptyMap();
		}

		final ConcurrentHashMap<String,NamedNativeQueryMemento> descriptorMap = new ConcurrentHashMap<>();

		for ( Map.Entry<String, NamedNativeQueryDefinition> entry : namedNativeQueryMap.entrySet() ) {
			descriptorMap.put(
					entry.getKey(),
					entry.getValue().resolve( sessionFactory )
			);
		}

		return descriptorMap;
	}

	private Map<String, ResultSetMappingDescriptor> buildSqlResultSetMappings(SessionFactoryImplementor sessionFactory) {
		if ( sqlResultSetMappingMap == null || sqlResultSetMappingMap.isEmpty() ) {
			return Collections.emptyMap();
		}

		final ConcurrentHashMap<String,ResultSetMappingDescriptor> converted = new ConcurrentHashMap<>();

		for ( Map.Entry<String, ResultSetMappingDefinition> bootEntry : sqlResultSetMappingMap.entrySet() ) {
			converted.put(
					bootEntry.getKey(),
					bootEntry.getValue().resolve( sessionFactory )
			);
		}

		return converted;
	}

	private Map<String, NamedCallableQueryMemento> buildProcedureCallMementos(SessionFactoryImplementor sessionFactory) {
		final Map<String, NamedCallableQueryMemento> rtn = new HashMap<>();
		if ( namedProcedureCallMap != null ) {
			for ( NamedProcedureCallDefinition procedureCallDefinition : namedProcedureCallMap.values() ) {
				rtn.put(
						procedureCallDefinition.getRegisteredName(),
						procedureCallDefinition.toMemento( sessionFactory )
				);
			}
		}
		return rtn;
	}

	@Override
	public void validate() throws MappingException {
		for ( EntityMapping entityBinding : getEntityMappings() ) {
			((EntityMappingImplementor)entityBinding).validate();
		}

		for ( Collection collectionBinding : this.getCollectionBindings() ) {
			collectionBinding.validate();
		}
	}

	@Override
	public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
		return mappedSuperclassMap == null
				? Collections.emptySet()
				: new HashSet<>( mappedSuperclassMap.values() );
	}

	@Override
	public AuditMetadataBuilderImplementor getAuditMetadataBuilder() {
		return auditMetadataBuilder;
	}
}
