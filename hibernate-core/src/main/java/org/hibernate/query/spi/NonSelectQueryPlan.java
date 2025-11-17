/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

/**
 * @author Steve Ebersole
 */
public interface NonSelectQueryPlan extends QueryPlan {
	int executeUpdate(DomainQueryExecutionContext executionContext);
}
