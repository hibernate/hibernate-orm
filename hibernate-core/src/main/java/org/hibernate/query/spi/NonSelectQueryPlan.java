/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

/**
 * @author Steve Ebersole
 */
public interface NonSelectQueryPlan extends QueryPlan {
	int executeUpdate(DomainQueryExecutionContext executionContext);
}
