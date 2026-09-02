/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.testing.SchemaGenerationResult;
import org.hibernate.dialect.testing.SqlGenerationRequest;
import org.hibernate.dialect.testing.SqlGenerationResult;

/// Internal bridge used by the public context facade.
///
/// @author Steve Ebersole
public interface DialectTestContextAccess extends AutoCloseable {
	Dialect getDialect();

	SqlGenerationResult translate(SqlGenerationRequest request);

	SchemaGenerationResult generateSchema();

	@Override
	void close();
}
