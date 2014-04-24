/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metamodel.NamedStoredProcedureQueryDefinition;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.TypeResolver;

/**
 * Container for configuration data collected during binding the metamodel.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class MetadataImpl implements MetadataImplementor, Serializable {
	private final StandardServiceRegistry serviceRegistry;

	private final Database database;
	private final TypeResolver typeResolver;
	private final IdentifierGeneratorFactory identifierGeneratorFactory;

	private final UUID uuid;

	private final Map<String, TypeDefinition> typeDefinitionMap = new HashMap<String, TypeDefinition>();
	private final Map<String, FilterDefinition> filterDefinitionMap = new HashMap<String, FilterDefinition>();
	private final Map<String, EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();
	private final Map<String, PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();
	private final Map<String, FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();
	private final Map<String, String> imports = new HashMap<String, String>();
	private final Map<String, IdentifierGeneratorDefinition> idGenerators = new HashMap<String, IdentifierGeneratorDefinition>();
	private final Map<String, NamedQueryDefinition> namedQueryDefs = new HashMap<String, NamedQueryDefinition>();
	private final Map<String, NamedSQLQueryDefinition> namedNativeQueryDefs = new HashMap<String, NamedSQLQueryDefinition>();
	private final Map<String, NamedStoredProcedureQueryDefinition> namedStoredProcedureQueryDefinitionMap = new HashMap<String, NamedStoredProcedureQueryDefinition>();
	private final Map<String, ResultSetMappingDefinition> resultSetMappings = new HashMap<String, ResultSetMappingDefinition>();
	private final Map<String, NamedEntityGraphDefinition> namedEntityGraphMap = new HashMap<String, NamedEntityGraphDefinition>(  );
	private final Map<Identifier, SecondaryTable> secondaryTableMap = new HashMap<Identifier, SecondaryTable>(  );

	MetadataImpl(
			StandardServiceRegistry serviceRegistry,
			Database database,
			TypeResolver typeResolver,
			UUID uuid,
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Map<String, TypeDefinition> typeDefinitionMap,
			Map<String, FilterDefinition> filterDefinitionMap,
			Map<String, EntityBinding> entityBindingMap,
			Map<String, PluralAttributeBinding> collectionBindingMap,
			Map<String, FetchProfile> fetchProfiles,
			Map<String, String> imports,
			Map<String, IdentifierGeneratorDefinition> idGenerators,
			Map<String, NamedQueryDefinition> namedQueryDefs,
			Map<String, NamedSQLQueryDefinition> namedNativeQueryDefs,
			Map<String, NamedStoredProcedureQueryDefinition> namedStoredProcedureQueryDefinitionMap,
			Map<String, ResultSetMappingDefinition> resultSetMappings,
			Map<String, NamedEntityGraphDefinition> namedEntityGraphMap,
			Map<Identifier, SecondaryTable> secondaryTableMap) {
		this.serviceRegistry = serviceRegistry;
		this.database = database;
		this.typeResolver = typeResolver;
		this.uuid = uuid;
		this.identifierGeneratorFactory = identifierGeneratorFactory;

		if ( typeDefinitionMap != null ) {
			this.typeDefinitionMap.putAll( typeDefinitionMap );
		}
		if ( filterDefinitionMap != null ) {
			this.filterDefinitionMap.putAll( filterDefinitionMap );
		}
		if ( entityBindingMap != null ) {
			this.entityBindingMap.putAll( entityBindingMap );
		}
		if ( collectionBindingMap != null ) {
			this.collectionBindingMap.putAll( collectionBindingMap );
		}
		if ( fetchProfiles != null ) {
			this.fetchProfiles.putAll( fetchProfiles );
		}
		if ( imports != null ) {
			this.imports.putAll( imports );
		}
		if ( idGenerators != null ) {
			this.idGenerators.putAll( idGenerators );
		}
		if ( namedQueryDefs != null ) {
			this.namedQueryDefs.putAll( namedQueryDefs );
		}
		if ( namedNativeQueryDefs != null ) {
			this.namedNativeQueryDefs.putAll( namedNativeQueryDefs );
		}
		if ( resultSetMappings != null ) {
			this.resultSetMappings.putAll( resultSetMappings );
		}
		if ( namedStoredProcedureQueryDefinitionMap != null ) {
			this.namedStoredProcedureQueryDefinitionMap.putAll( namedStoredProcedureQueryDefinitionMap );
		}
		if ( namedEntityGraphMap != null ) {
			this.namedEntityGraphMap.putAll( namedEntityGraphMap );
		}
		if ( secondaryTableMap != null ) {
			this.secondaryTableMap.putAll( secondaryTableMap );
		}
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public TypeResolver getTypeResolver() {
		return typeResolver;
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

	@Override
	public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs() {
		return namedEntityGraphMap;
	}

	@Override
	public Map<String, FilterDefinition> getFilterDefinitions() {
		return filterDefinitionMap;
	}

	@Override
	public IdentifierGeneratorDefinition getIdGenerator(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid generator name" );
		}
		return idGenerators.get( name );
	}

	@Override
	public Iterable<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions() {
		return namedNativeQueryDefs.values();
	}

	@Override
	public Collection<NamedStoredProcedureQueryDefinition> getNamedStoredProcedureQueryDefinitions() {
		return namedStoredProcedureQueryDefinitionMap.values();
	}

	@Override
	public Iterable<NamedQueryDefinition> getNamedQueryDefinitions() {
		return namedQueryDefs.values();
	}

	@Override
	public NamedSQLQueryDefinition getNamedNativeQuery(String name) {
		return namedNativeQueryDefs.get( name );
	}

	@Override
	public Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions() {
		return resultSetMappings;
	}

	@Override
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
	public Map<Identifier, SecondaryTable> getSecondaryTables() {
		return secondaryTableMap;
	}

	@Override
	public Iterable<PluralAttributeBinding> getCollectionBindings() {
		return collectionBindingMap.values();
	}

	public PluralAttributeBinding getCollection(String role) {
		return collectionBindingMap.get( role );
	}

	@Override
	public Map<String,String> getImports() {
		return imports;
	}

	@Override
	public Iterable<FetchProfile> getFetchProfiles() {
		return fetchProfiles.values();
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		return new SessionFactoryBuilderImpl( this, serviceRegistry );
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return getSessionFactoryBuilder().build();
	}

	@Override
	public UUID getUUID() {
		return null;
	}

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
		return entityBinding.getHierarchyDetails()
				.getEntityIdentifier()
				.getEntityIdentifierBinding()
				.getHibernateType();
	}

	@Override
	public String getIdentifierPropertyName(String entityName) throws MappingException {
		EntityBinding entityBinding = getEntityBinding( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}

		final EntityIdentifier idInfo = entityBinding.getHierarchyDetails().getEntityIdentifier();
		if ( idInfo.getNature() == EntityIdentifierNature.NON_AGGREGATED_COMPOSITE ) {
			return null;
		}

		final EntityIdentifier.AttributeBasedIdentifierBinding identifierBinding =
				(EntityIdentifier.AttributeBasedIdentifierBinding) idInfo.getEntityIdentifierBinding();
		return identifierBinding.getAttributeBinding().getAttribute().getName();
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
}
