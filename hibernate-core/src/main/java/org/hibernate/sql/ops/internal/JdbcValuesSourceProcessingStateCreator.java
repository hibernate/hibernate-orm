/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;

/**
 * @author Steve Ebersole
 */
public interface JdbcValuesSourceProcessingStateCreator {
	JdbcValuesSourceProcessingState createState(
			ExecutionContext executionContext,
			JdbcValuesSourceProcessingOptions processingOptions);
}
