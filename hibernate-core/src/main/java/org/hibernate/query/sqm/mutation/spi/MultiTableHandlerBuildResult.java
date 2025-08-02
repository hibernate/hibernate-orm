/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.spi;

import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * The build result of a {@link MultiTableHandler}.
 *
 * @see SqmMultiTableMutationStrategy#buildHandler(SqmDeleteOrUpdateStatement, DomainParameterXref, DomainQueryExecutionContext)
 * @see SqmMultiTableInsertStrategy#buildHandler(SqmInsertStatement, DomainParameterXref, DomainQueryExecutionContext)
 * @since 7.1
 */
public record MultiTableHandlerBuildResult(
		MultiTableHandler multiTableHandler,
		JdbcParameterBindings firstJdbcParameterBindings) {

}
