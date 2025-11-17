/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.Statement;

/**
 * @author Steve Ebersole
 */
public interface SqmTranslator<T extends Statement>
		extends SqmToSqlAstConverter, FromClauseAccess, JdbcParameterBySqmParameterAccess {
	SqmTranslation<T> translate();
}
