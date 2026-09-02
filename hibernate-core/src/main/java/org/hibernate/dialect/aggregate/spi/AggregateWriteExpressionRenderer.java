/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Renders one aggregate-column custom write expression.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see AggregateSupport#aggregateCustomWriteExpressionRenderer(AggregateWriteRendererRequest)
@FunctionalInterface
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface AggregateWriteExpressionRenderer {
	void render(SqlAppender sqlAppender, SqlAstTranslator<?> translator,
			AggregateColumnWriteExpression writeExpression, String qualifier);
}
