/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.SqlTypedMapping;

/**
 * An expression that has SQL type information.
 */
public interface SqlTypedExpression extends Expression {
	SqlTypedMapping getSqlTypedMapping();
}
