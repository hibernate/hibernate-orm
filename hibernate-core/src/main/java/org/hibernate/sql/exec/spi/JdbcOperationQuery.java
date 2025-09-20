/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.query.Query;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

import java.util.Map;

/**
 * A JdbcOperation which (possibly) originated from a {@linkplain Query}.
 *
 * @author Steve Ebersole
 */
public interface JdbcOperationQuery extends JdbcOperation, CacheableJdbcOperation {
	/**
	 * The parameters which were inlined into the query as literals.
	 *
	 * @deprecated No longer called
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	Map<JdbcParameter, JdbcParameterBinding> getAppliedParameters();
}
