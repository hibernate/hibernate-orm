/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import java.util.Map;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

/// Collector for completed boot metadata contributions produced during binding.
///
/// This contract is deliberately narrower than ORM's current
/// `InFlightMetadataCollector`.  `BindingState` owns mutable binding-phase
/// working state; `MetadataCollector` owns writes to the metadata product being
/// assembled.
///
/// The current implementation is an adapter over `InFlightMetadataCollector`.
/// That makes the boundary explicit while keeping ORM's existing metadata
/// finalization path intact.
///
/// @since 9.0
/// @author Steve Ebersole
public interface MetadataCollector {
	/// Register an entity binding.
	void addEntityBinding(PersistentClass entityBinding);

	/// Register a mapped-superclass binding.
	void addMappedSuperclass(Class<?> mappedSuperclassClass, MappedSuperclass mappedSuperclass);

	/// Register a collection binding.
	void addCollectionBinding(Collection collection);

	/// Register an entity-name import.
	void addImport(String importName, String entityName);

	/// Register a unique property reference.
	void addUniquePropertyReference(String referencedEntityName, String referencedPropertyName);

	/// Register an identifier generator.
	void addIdentifierGenerator(IdentifierGeneratorDefinition identifierGeneratorDefinition);

	/// Register a named entity graph.
	void addNamedEntityGraph(NamedEntityGraphDefinition namedEntityGraphDefinition);

	/// Register a named SQL result set mapping.
	void addResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDescriptor);

	/// Register a fetch profile.
	void addFetchProfile(FetchProfile fetchProfile);

	/// Resolve a fetch profile already published to the metadata collector.
	FetchProfile getFetchProfile(String name);

	/// Register an auxiliary database object.
	void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	/// Register an auto-apply converter.
	void addAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass);

	/// Register an explicit converter.
	void addRegisteredConversion(RegisteredConversion registeredConversion);

	/// Register a Java type descriptor.
	void addJavaTypeRegistration(Class<?> domainType, JavaType<?> descriptor);

	/// Register a JDBC type descriptor.
	void addJdbcTypeRegistration(int code, JdbcType descriptor);

	/// Register a custom user type.
	void registerUserType(Class<?> domainClass, Class<? extends UserType<?>> userTypeClass);

	/// Register a custom composite user type.
	void registerCompositeUserType(Class<?> embeddableClass, Class<? extends CompositeUserType<?>> userTypeClass);

	/// Register a collection type.
	void addCollectionTypeRegistration(
			CollectionClassification classification,
			Class<? extends UserCollectionType> userTypeClass,
			Map<String,String> parameters);

	/// Register an embeddable instantiator.
	void registerEmbeddableInstantiator(
			Class<?> embeddableClass,
			Class<? extends EmbeddableInstantiator> instantiatorClass);

	/// Resolve a filter definition already published to the metadata collector.
	FilterDefinition getFilterDefinition(String name);

	/// Register a filter definition.
	void addFilterDefinition(FilterDefinition filterDefinition);
}
