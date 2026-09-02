/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import static org.hibernate.SPI.Role.USE;

/// Resolves the value assigned to one aggregate selectable.
///
/// @author Steve Ebersole
/// @since 8.0
@FunctionalInterface
@SPI(USE)
public interface AggregateColumnWriteExpression {
	Expression getValueExpression(SelectableMapping selectableMapping);
}
