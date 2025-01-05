/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import org.hibernate.Incubating;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;

/**
 * Introduced as an analog of {@link org.hibernate.query.spi.QueryEngine}
 * and/or {@link org.hibernate.query.sqm.NodeBuilder} for the SQL
 * translation and rendering phases. For now there is nothing here.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
@Incubating
public interface SqlTranslationEngine extends SqlAstCreationContext {
	// TODO: consider implementing SqlStringGenerationContext
}
