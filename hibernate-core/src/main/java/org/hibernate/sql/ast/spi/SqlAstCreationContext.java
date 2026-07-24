/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.query.sql.spi.SqlTranslationContext;

/**
 * The "context" in which creation of SQL AST occurs. Provides
 * access to stuff generally needed when creating SQL AST nodes
 * <p>
 * Because we would like to be able to render SQL during the
 * startup cycle, before the {@code SessionFactory} is completely
 * initialized, code involved in SQL AST creation and rendering
 * should avoid making use of the {@code SessionFactory}.
 * Instead, use the objects exposed by this creation context.
 *
 * @author Steve Ebersole
 */
public interface SqlAstCreationContext extends SqlTranslationContext {
}
