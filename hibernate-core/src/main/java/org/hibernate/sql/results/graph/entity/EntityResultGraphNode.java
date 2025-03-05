/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.graph.spi.GraphHelper;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.sql.results.graph.DomainResultGraphNode;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Represents a reference to an entity either as a return, fetch, or collection element or index.
 *
 * @author Steve Ebersole
 */
public interface EntityResultGraphNode extends DomainResultGraphNode, FetchParent {
	@Override
	NavigablePath getNavigablePath();

	EntityValuedModelPart getEntityValuedModelPart();

	@Override
	default boolean containsAnyNonScalarResults() {
		return true;
	}

	@Override
	default JavaType<?> getResultJavaType() {
		return getEntityValuedModelPart().getEntityMappingType().getMappedJavaType();
	}

	@Override
	default EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	default boolean appliesTo(GraphImplementor<?> graphImplementor, JpaMetamodel metamodel) {
		final String entityName = getEntityValuedModelPart().getEntityMappingType().getEntityName();
		return GraphHelper.appliesTo( graphImplementor, metamodel.entity( entityName ) );
	}
}
