/*
 * SPDX-License-Identifier: Apache-2.0
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
