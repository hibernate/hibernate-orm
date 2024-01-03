/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Registrations which are considered global, collected across annotations
 * and XML mappings.
 *
 * @author Steve Ebersole
 */
public interface GlobalRegistrations {
	List<JpaEventListener> getEntityListenerRegistrations();

	List<ConversionRegistration> getConverterRegistrations();

	List<JavaTypeRegistration> getJavaTypeRegistrations();

	List<JdbcTypeRegistration> getJdbcTypeRegistrations();

	List<UserTypeRegistration> getUserTypeRegistrations();

	List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations();

	List<CollectionTypeRegistration> getCollectionTypeRegistrations();

	List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations();

	Map<String, FilterDefRegistration> getFilterDefRegistrations();

	Map<String, String> getImportedRenames();

	Map<String, SequenceGeneratorRegistration> getSequenceGeneratorRegistrations();

	Map<String, TableGeneratorRegistration> getTableGeneratorRegistrations();

	Map<String, GenericGeneratorRegistration> getGenericGeneratorRegistrations();

	Set<ConverterRegistration> getJpaConverters();

	// todo : named entity graphs
	// todo : named queries
}
