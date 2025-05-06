/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public interface DynamicResultBuilderEntity extends DynamicResultBuilder, ResultBuilderEntityValued {
	@Override
	EntityResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState);
}
