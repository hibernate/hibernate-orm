/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.creation;

import org.hibernate.sql.ast.spi.query.expression.Expression;

/**
 * @author Steve Ebersole
 */
public interface SqlExpressionAccess {
	Expression getSqlExpression();
}
