/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Dialect strategy for selecting immutable rendering plans for query-language
/// update and delete statements.
///
/// Custom translators should normally reuse
/// [StandardQueryMutationRenderingSupport] or subclass a supported translator
/// family which already supplies the required behavior. Implement this contract
/// when plan selection needs different semantic rules. Implementations must base
/// selection only on the supplied read-only request and must not retain
/// translator or statement state.
///
/// @see AbstractSqlAstTranslator#getQueryMutationRenderingSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ IMPLEMENT, SUPPLY })
public interface QueryMutationRenderingSupport {
	/// Select a non-null native or emulation plan for a query update.
	UpdateRenderingPlan determineUpdatePlan(UpdateRenderingRequest request);

	/// Select a non-null native or emulation plan for a query delete.
	DeleteRenderingPlan determineDeletePlan(DeleteRenderingRequest request);
}
