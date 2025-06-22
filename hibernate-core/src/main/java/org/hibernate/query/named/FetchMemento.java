/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface FetchMemento extends ModelPartReferenceMemento {
	/**
	 * The parent node for the fetch
	 */
	interface Parent extends ModelPartReferenceMemento {
	}

	/**
	 * Resolve the fetch-memento into the result-graph-node builder
	 */
	FetchBuilder resolve(Parent parent, Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context);
}
