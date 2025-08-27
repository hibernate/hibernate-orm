/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.type.BindingContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;

/**
 * The context in which all SQM creations occur.
 * <p>
 * Since we need to be able to parse and type check queries completely
 * outside the usual lifecycle of a Hibernate {@code SessionFactory},
 * it's extremely important that code which builds SQM trees does not
 * access the factory or other services or object not exposed by this
 * context object.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationContext extends BindingContext {

	/**
	 * The {@link QueryEngine}.
	 */
	QueryEngine getQueryEngine();

	/**
	 * The {@link NodeBuilder}.
	 */
	default NodeBuilder getNodeBuilder() {
		return getQueryEngine().getCriteriaBuilder();
	}

	/**
	 * Obtain a Java class object with the given fully-qualified
	 * name. This method may only be used for unmanaged types,
	 * for example, for {@code select new}, or for references to
	 * {@code static final} constants or to {@code enum} values.
	 *
	 * @apiNote Avoid calling this method, since {@link Class}
	 *          objects are not available to the query validator
	 *          in Hibernate Processor at compilation time. If
	 *          you must call it, be prepared to robustly handle
	 *          the case in which the class is not present, in
	 *          which case this method might return something
	 *          arbitrary like {@code Object[].class}.
	 */
	default Class<?> classForName(String className) {
		return getQueryEngine().getClassLoaderService().classForName( className );
	}

	/**
	 * The {@link MappingMetamodel}.
	 */
	@Override
	default MappingMetamodel getMappingMetamodel() {
		return getQueryEngine().getMappingMetamodel();
	}

	/**
	 * The {@link JpaMetamodel}.
	 */
	@Override
	default JpaMetamodel getJpaMetamodel() {
		return getQueryEngine().getJpaMetamodel();
	}
}
