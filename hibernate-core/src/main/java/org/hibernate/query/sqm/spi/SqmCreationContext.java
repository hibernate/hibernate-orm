/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.BindingContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;

/**
 * The context in which all SQM creations occur.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationContext extends BindingContext {
	QueryEngine getQueryEngine();

	default NodeBuilder getNodeBuilder() {
		return getQueryEngine().getCriteriaBuilder();
	}

	/**
	 * @apiNote Avoid calling this method, since {@link Class}
	 *          objects are not available to the query validator
	 *          in Hibernate Processor at compilation time.
	 */
	default Class<?> classForName(String className) {
		return getQueryEngine().getClassLoaderService().classForName( className );
	}

	@Override
	default MappingMetamodel getMappingMetamodel() {
		return getQueryEngine().getMappingMetamodel();
	}

	@Override
	default JpaMetamodel getJpaMetamodel() {
		return getQueryEngine().getJpaMetamodel();
	}
}
