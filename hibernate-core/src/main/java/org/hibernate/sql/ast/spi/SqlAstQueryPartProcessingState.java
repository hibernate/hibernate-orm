/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;


import org.hibernate.sql.ast.tree.select.QueryPart;

/**
 * SqlAstProcessingState specialization for query parts
 *
 * @author Steve Ebersole
 */
public interface SqlAstQueryPartProcessingState extends SqlAstQueryNodeProcessingState {
	/**
	 * Get the QueryPart being processed as part of this state.  It is
	 * considered in-flight as it is probably still being built.
	 */
	QueryPart getInflightQueryPart();
}
