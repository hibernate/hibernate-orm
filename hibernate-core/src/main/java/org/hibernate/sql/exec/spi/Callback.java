/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.AfterLoadAction;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * Callback to allow SQM interpretation to trigger certain things within ORM.  See the current
 * {@link AfterLoadAction} javadocs for details.  Specifically this would
 * encompass things like follow-on locking, follow-on fetching, etc.
 *
 * @author Steve Ebersole
 */
@Incubating(since = "6.0", group = "sql-execution")
public interface Callback {
	/**
	 * Register a callback action
	 */
	void registerAfterLoadAction(AfterLoadAction afterLoadAction);

	/**
	 * Invoke all {@linkplain #registerAfterLoadAction registered} actions
	 */
	void invokeAfterLoadActions(Object entity, EntityMappingType entityMappingType, SharedSessionContractImplementor session);

	boolean hasAfterLoadActions();
}
