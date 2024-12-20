/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;

/**
 * @see SqlScriptCommandExtractor
 *
 * @deprecated Use {@link SqlScriptCommandExtractor} instead.
 */
@Deprecated
public interface ImportSqlCommandExtractor extends SqlScriptCommandExtractor {
}
