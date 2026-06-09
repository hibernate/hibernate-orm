/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.NamedEntityGraphDefinition;


/// Persistence-unit scoped registrations collected while categorizing annotations
/// and XML mappings.
///
/// These registrations are not owned by a single managed type or attribute.  They
/// contribute named or type-based services to later boot phases, such as converters,
/// custom type descriptors, filter definitions, and identifier generators.  The
/// categorized domain model exposes them alongside the persistent type model because
/// both are produced from the same source interpretation step.
///
/// @since 9.0
/// @author Steve Ebersole
public interface GlobalRegistrations {
	/// Entity listener registrations declared for the persistence unit.
	List<JpaEventListener> getEntityListenerRegistrations();

	/// Attribute converter registrations.
	List<ConversionRegistration> getConverterRegistrations();

	/// JPA attribute converter classes.
	Set<JpaConverterRegistration> getJpaConverters();

	/// Java type descriptor registrations.
	List<JavaTypeRegistration> getJavaTypeRegistrations();

	/// JDBC type descriptor registrations.
	List<JdbcTypeRegistration> getJdbcTypeRegistrations();

	/// User type registrations.
	List<UserTypeRegistration> getUserTypeRegistrations();

	/// Composite user type registrations.
	List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations();

	/// Collection type registrations.
	List<CollectionTypeRegistration> getCollectionTypeRegistrations();

	/// Embeddable instantiator registrations.
	List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations();

	/// Filter definitions keyed by filter name.
	Map<String, FilterDefRegistration> getFilterDefRegistrations();

	/// Fetch profile definitions.
	List<FetchProfileRegistration> getFetchProfileRegistrations();

	/// HQL import aliases keyed by alias.
	Map<String, String> getImportedRenames();

	/// Sequence generator definitions keyed by generator name.
	Map<String, SequenceGeneratorRegistration> getSequenceGeneratorRegistrations();

	/// Table generator definitions keyed by generator name.
	Map<String, TableGeneratorRegistration> getTableGeneratorRegistrations();

	/// Generic generator definitions keyed by generator name.
	Map<String, GenericGeneratorRegistration> getGenericGeneratorRegistrations();

	/// SQL result set mappings keyed by mapping name.
	Map<String, SqlResultSetMappingRegistration> getSqlResultSetMappingRegistrations();

	/// Named HQL/query definitions keyed by query name.
	Map<String, NamedQueryRegistration> getNamedQueryRegistrations();

	/// Named native SQL query definitions keyed by query name.
	Map<String, NamedQueryRegistration> getNamedNativeQueryRegistrations();

	/// Named stored procedure query definitions keyed by query name.
	Map<String, NamedQueryRegistration> getNamedStoredProcedureQueryRegistrations();

	/// Named entity graph definitions keyed by graph name.
	Map<String, NamedEntityGraphDefinition> getNamedEntityGraphRegistrations();

	/// Auxiliary database object registrations.
	List<DatabaseObjectRegistration> getDatabaseObjectRegistrations();
}
