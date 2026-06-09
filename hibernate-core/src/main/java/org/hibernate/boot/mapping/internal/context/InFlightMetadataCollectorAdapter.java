/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import java.util.Map;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

/// `MetadataCollector` adapter backed by ORM's current mutable metadata collector.
///
/// This is a compatibility adapter, not the desired long-term collector.  It keeps
/// collector writes behind a small boot-model contract while the PoC still uses
/// ORM's `InFlightMetadataCollectorImpl` to produce `MetadataImplementor`.
///
/// @since 9.0
/// @author Steve Ebersole
public class InFlightMetadataCollectorAdapter implements MetadataCollector {
	private final InFlightMetadataCollector metadataCollector;

	public InFlightMetadataCollectorAdapter(InFlightMetadataCollector metadataCollector) {
		this.metadataCollector = metadataCollector;
	}

	@Override
	public Database getDatabase() {
		return metadataCollector.getDatabase();
	}

	@Override
	public Table getOrCreateTable(
			String schema,
			String catalog,
			String name,
			String subselect,
			boolean isAbstract,
			org.hibernate.boot.spi.MetadataBuildingContext buildingContext,
			boolean isExplicit) {
		return metadataCollector.addTable( schema, catalog, name, subselect, isAbstract, buildingContext, isExplicit );
	}

	@Override
	public DenormalizedTable createDenormalizedTable(
			String schema,
			String catalog,
			String name,
			boolean isAbstract,
			String subselect,
			Table includedTable,
			org.hibernate.boot.spi.MetadataBuildingContext buildingContext) {
		return (DenormalizedTable) metadataCollector.addDenormalizedTable(
				schema,
				catalog,
				name,
				isAbstract,
				subselect,
				includedTable,
				buildingContext
		);
	}

	@Override
	public void addEntityBinding(PersistentClass entityBinding) {
		metadataCollector.addEntityBinding( entityBinding );
	}

	@Override
	public PersistentClass getEntityBinding(String entityName) {
		return metadataCollector.getEntityBinding( entityName );
	}

	@Override
	public Iterable<PersistentClass> getEntityBindings() {
		return metadataCollector.getEntityBindings();
	}

	@Override
	public void addMappedSuperclass(Class<?> mappedSuperclassClass, MappedSuperclass mappedSuperclass) {
		metadataCollector.addMappedSuperclass( mappedSuperclassClass, mappedSuperclass );
	}

	@Override
	public void addCollectionBinding(Collection collection) {
		metadataCollector.addCollectionBinding( collection );
	}

	@Override
	public void addImport(String importName, String entityName) {
		metadataCollector.addImport( importName, entityName );
	}

	@Override
	public String getImport(String importName) {
		return metadataCollector.getImports().get( importName );
	}

	@Override
	public void addUniquePropertyReference(String referencedEntityName, String referencedPropertyName) {
		metadataCollector.addUniquePropertyReference( referencedEntityName, referencedPropertyName );
	}

	@Override
	public void addPropertyReference(String referencedEntityName, String referencedPropertyName) {
		metadataCollector.addPropertyReference( referencedEntityName, referencedPropertyName );
	}

	@Override
	public void addIdentifierGenerator(IdentifierGeneratorDefinition identifierGeneratorDefinition) {
		metadataCollector.addIdentifierGenerator( identifierGeneratorDefinition );
	}

	@Override
	public void addNamedEntityGraph(NamedEntityGraphDefinition namedEntityGraphDefinition) {
		metadataCollector.addNamedEntityGraph( namedEntityGraphDefinition );
	}

	@Override
	public void addResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDescriptor) {
		metadataCollector.addResultSetMapping( resultSetMappingDescriptor );
	}

	@Override
	public void addFetchProfile(FetchProfile fetchProfile) {
		metadataCollector.addFetchProfile( fetchProfile );
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return metadataCollector.getFetchProfile( name );
	}

	@Override
	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		metadataCollector.addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
	}

	@Override
	public void addAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
		metadataCollector.addAttributeConverter( converterClass );
	}

	@Override
	public void addAttributeConverter(ConverterDescriptor<?, ?> converterDescriptor) {
		metadataCollector.addAttributeConverter( converterDescriptor );
	}

	@Override
	public void addRegisteredConversion(RegisteredConversion registeredConversion) {
		metadataCollector.addRegisteredConversion( registeredConversion );
	}

	@Override
	public void addJavaTypeRegistration(Class<?> domainType, JavaType<?> descriptor) {
		metadataCollector.addJavaTypeRegistration( domainType, descriptor );
	}

	@Override
	public void addJdbcTypeRegistration(int code, JdbcType descriptor) {
		metadataCollector.addJdbcTypeRegistration( code, descriptor );
	}

	@Override
	public void registerUserType(Class<?> domainClass, Class<? extends UserType<?>> userTypeClass) {
		metadataCollector.registerUserType( domainClass, userTypeClass );
	}

	@Override
	public Class<? extends UserType<?>> findRegisteredUserType(Class<?> domainClass) {
		return metadataCollector.findRegisteredUserType( domainClass );
	}

	@Override
	public void registerCompositeUserType(
			Class<?> embeddableClass,
			Class<? extends CompositeUserType<?>> userTypeClass) {
		metadataCollector.registerCompositeUserType( embeddableClass, userTypeClass );
	}

	@Override
	public Class<? extends CompositeUserType<?>> findRegisteredCompositeUserType(Class<?> embeddableClass) {
		return metadataCollector.findRegisteredCompositeUserType( embeddableClass );
	}

	@Override
	public void addCollectionTypeRegistration(
			CollectionClassification classification,
			Class<? extends UserCollectionType> userTypeClass,
			Map<String, String> parameters) {
		metadataCollector.addCollectionTypeRegistration(
				classification,
				new InFlightMetadataCollector.CollectionTypeRegistrationDescriptor(
						userTypeClass,
						parameters
				)
		);
	}

	@Override
	public InFlightMetadataCollector.CollectionTypeRegistrationDescriptor findCollectionTypeRegistration(
			CollectionClassification classification) {
		return metadataCollector.findCollectionTypeRegistration( classification );
	}

	@Override
	public void registerEmbeddableInstantiator(
			Class<?> embeddableClass,
			Class<? extends EmbeddableInstantiator> instantiatorClass) {
		metadataCollector.registerEmbeddableInstantiator( embeddableClass, instantiatorClass );
	}

	@Override
	public FilterDefinition getFilterDefinition(String name) {
		return metadataCollector.getFilterDefinition( name );
	}

	@Override
	public void addFilterDefinition(FilterDefinition filterDefinition) {
		metadataCollector.addFilterDefinition( filterDefinition );
	}
}
