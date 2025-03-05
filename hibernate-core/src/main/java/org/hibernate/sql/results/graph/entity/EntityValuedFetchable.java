/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.engine.FetchTiming;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.spi.NavigablePath;

/**
 * Fetchable which is entity-valued
 *
 * @author Steve Ebersole
 */
public interface EntityValuedFetchable extends Fetchable, EntityValuedModelPart {
	@Override
	EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState);

	boolean isOptional();

	boolean isUnwrapProxy();
}
