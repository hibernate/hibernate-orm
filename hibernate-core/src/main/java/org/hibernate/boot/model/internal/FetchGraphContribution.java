/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.FetchOption;

/**
 * Models a single {@link jakarta.persistence.Fetch} declaration as a contribution
 * to a JPA named entity graph.
 * <p>
 * The contribution identifies the declaring attribute, the named graph targeted
 * by {@link jakarta.persistence.Fetch#graph()}, the optional named subgraphs
 * targeted by {@link jakarta.persistence.Fetch#subgraph()}, and the collected
 * {@link FetchOption}s to apply to the resulting graph attribute node.
 * <p>
 * This is boot-time metadata only. It does not represent static mapping fetch
 * metadata and is consumed while creating JPA named entity graphs.
 *
 * @since 8.0
 * @author Steve Ebersole
 */
record FetchGraphContribution(
		String graphName,
		String attributeName,
		String[] subgraphNames,
		List<FetchOption> options) implements Serializable {
	boolean appliesTo(String namedGraph) {
		return graphName.equals( namedGraph );
	}

	boolean appliesToSubgraph(String subgraphName) {
		if ( subgraphNames.length == 0 ) {
			return subgraphName == null;
		}
		if ( subgraphName == null ) {
			return false;
		}
		for ( var name : subgraphNames ) {
			if ( subgraphName.equals( name ) ) {
				return true;
			}
		}
		return false;
	}
}
