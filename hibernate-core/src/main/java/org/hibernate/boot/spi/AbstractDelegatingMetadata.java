/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.cfg.annotations.NamedProcedureCallDefinition;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

/**
 * Convenience base class for custom implementors of {@link MetadataImplementor} using delegation.
 *
 * @author Gunnar Morling
 *
 */
public abstract class AbstractDelegatingMetadata implements MetadataImplementor {

	private final MetadataImplementor delegate;

	public AbstractDelegatingMetadata(MetadataImplementor delegate) {
		this.delegate = delegate;
	}

	@Override
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return delegate.getIdentifierGeneratorFactory();
	}

	@Override
	public Type getIdentifierType(String className) throws MappingException {
		return delegate.getIdentifierType( className );
	}

	@Override
	public String getIdentifierPropertyName(String className) throws MappingException {
		return delegate.getIdentifierPropertyName( className );
	}

	@Override
	public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
		return delegate.getReferencedPropertyType( className, propertyName );
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		return delegate.getSessionFactoryBuilder();
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return delegate.buildSessionFactory();
	}

	@Override
	public UUID getUUID() {
		return delegate.getUUID();
	}

	@Override
	public Database getDatabase() {
		return delegate.getDatabase();
	}

	@Override
	public Collection<PersistentClass> getEntityBindings() {
		return delegate.getEntityBindings();
	}

	@Override
	public PersistentClass getEntityBinding(String entityName) {
		return delegate.getEntityBinding( entityName );
	}

	@Override
	public Collection<org.hibernate.mapping.Collection> getCollectionBindings() {
		return delegate.getCollectionBindings();
	}

	@Override
	public org.hibernate.mapping.Collection getCollectionBinding(String role) {
		return delegate.getCollectionBinding( role );
	}

	@Override
	public Map<String, String> getImports() {
		return delegate.getImports();
	}

	@Override
	public NamedQueryDefinition getNamedQueryDefinition(String name) {
		return delegate.getNamedQueryDefinition( name );
	}

	@Override
	public Collection<NamedQueryDefinition> getNamedQueryDefinitions() {
		return delegate.getNamedQueryDefinitions();
	}

	@Override
	public NamedSQLQueryDefinition getNamedNativeQueryDefinition(String name) {
		return delegate.getNamedNativeQueryDefinition( name );
	}

	@Override
	public Collection<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions() {
		return delegate.getNamedNativeQueryDefinitions();
	}

	@Override
	public Collection<NamedProcedureCallDefinition> getNamedProcedureCallDefinitions() {
		return delegate.getNamedProcedureCallDefinitions();
	}

	@Override
	public ResultSetMappingDefinition getResultSetMapping(String name) {
		return delegate.getResultSetMapping( name );
	}

	@Override
	public Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions() {
		return delegate.getResultSetMappingDefinitions();
	}

	@Override
	public TypeDefinition getTypeDefinition(String typeName) {
		return delegate.getTypeDefinition( typeName );
	}

	@Override
	public Map<String, FilterDefinition> getFilterDefinitions() {
		return delegate.getFilterDefinitions();
	}

	@Override
	public FilterDefinition getFilterDefinition(String name) {
		return delegate.getFilterDefinition( name );
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return delegate.getFetchProfile( name );
	}

	@Override
	public Collection<FetchProfile> getFetchProfiles() {
		return delegate.getFetchProfiles();
	}

	@Override
	public NamedEntityGraphDefinition getNamedEntityGraph(String name) {
		return delegate.getNamedEntityGraph( name );
	}

	@Override
	public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs() {
		return delegate.getNamedEntityGraphs();
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGenerator(String name) {
		return delegate.getIdentifierGenerator( name );
	}

	@Override
	public Collection<Table> collectTableMappings() {
		return delegate.collectTableMappings();
	}

	@Override
	public Map<String, SQLFunction> getSqlFunctionMap() {
		return delegate.getSqlFunctionMap();
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return delegate.getMetadataBuildingOptions();
	}

	@Override
	public TypeResolver getTypeResolver() {
		return delegate.getTypeResolver();
	}

	@Override
	public NamedQueryRepository buildNamedQueryRepository(SessionFactoryImpl sessionFactory) {
		return delegate.buildNamedQueryRepository( sessionFactory );
	}

	@Override
	public void validate() throws MappingException {
		delegate.validate();
	}

	@Override
	public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
		return delegate.getMappedSuperclassMappingsCopy();
	}
}
