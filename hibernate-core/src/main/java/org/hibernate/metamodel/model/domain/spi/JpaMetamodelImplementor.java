/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.spi;

import jakarta.persistence.EntityGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;

import java.util.List;
import java.util.Map;

/**
 * SPI extending {@link JpaMetamodel}.
 *
 * @author Steve Ebersole
 */
public interface JpaMetamodelImplementor extends JpaMetamodel {
	MappingMetamodel getMappingMetamodel();

	RootGraphImplementor<?> findEntityGraphByName(String name);

	<T> List<EntityGraph<? super T>> findEntityGraphsByJavaType(Class<T> entityClass);
	<T> Map<String, EntityGraph<? extends T>> getNamedEntityGraphs(Class<T> entityClass);
}
