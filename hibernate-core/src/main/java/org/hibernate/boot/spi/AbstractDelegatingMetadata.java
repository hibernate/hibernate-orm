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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

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

	protected MetadataImplementor delegate() {
		return delegate;
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
	public NamedHqlQueryDefinition getNamedHqlQueryMapping(String name) {
		return delegate.getNamedHqlQueryMapping( name );
	}

	@Override
	public void visitNamedHqlQueryDefinitions(Consumer<NamedHqlQueryDefinition> definitionConsumer) {
		delegate.visitNamedHqlQueryDefinitions( definitionConsumer );
	}

	@Override
	public NamedNativeQueryDefinition getNamedNativeQueryMapping(String name) {
		return delegate.getNamedNativeQueryMapping( name );
	}

	@Override
	public void visitNamedNativeQueryDefinitions(Consumer<NamedNativeQueryDefinition> definitionConsumer) {
		delegate.visitNamedNativeQueryDefinitions( definitionConsumer );
	}

	@Override
	public NamedProcedureCallDefinition getNamedProcedureCallMapping(String name) {
		return delegate.getNamedProcedureCallMapping( name );
	}

	@Override
	public void visitNamedProcedureCallDefinition(Consumer<NamedProcedureCallDefinition> definitionConsumer) {
		delegate.visitNamedProcedureCallDefinition( definitionConsumer );
	}

	@Override
	public NamedResultSetMappingDescriptor getResultSetMapping(String name) {
		return delegate.getResultSetMapping( name );
	}

	@Override
	public void visitNamedResultSetMappingDefinition(Consumer<NamedResultSetMappingDescriptor> definitionConsumer) {
		delegate.visitNamedResultSetMappingDefinition( definitionConsumer );
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
	public Map<String, SqmFunctionDescriptor> getSqlFunctionMap() {
		return delegate.getSqlFunctionMap();
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return delegate.getMetadataBuildingOptions();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return delegate.getTypeConfiguration();
	}

	@Override
	public SqmFunctionRegistry getFunctionRegistry() {
		return delegate.getFunctionRegistry();
	}

	@Override
	public void orderColumns(boolean forceOrdering) {
		delegate.orderColumns( false );
	}

	@Override
	public void validate() throws MappingException {
		delegate.validate();
	}

	@Override
	public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
		return delegate.getMappedSuperclassMappingsCopy();
	}

	@Override
	public void initSessionFactory(SessionFactoryImplementor sessionFactory) {
		delegate.initSessionFactory( sessionFactory );
	}

	@Override
	public void visitRegisteredComponents(Consumer<Component> consumer) {
		delegate().visitRegisteredComponents( consumer );
	}

	@Override
	public Component getGenericComponent(Class<?> componentClass) {
		return delegate().getGenericComponent( componentClass );
	}

	@Override
	public DiscriminatorType<?> resolveEmbeddableDiscriminatorType(
			Class<?> embeddableClass,
			Supplier<DiscriminatorType<?>> supplier) {
		return delegate().resolveEmbeddableDiscriminatorType( embeddableClass, supplier );
	}

	@Override
	public NamedObjectRepository buildNamedQueryRepository(SessionFactoryImplementor sessionFactory) {
		return delegate().buildNamedQueryRepository( sessionFactory );
	}

	@Override
	public Set<String> getContributors() {
		return delegate.getContributors();
	}
}
