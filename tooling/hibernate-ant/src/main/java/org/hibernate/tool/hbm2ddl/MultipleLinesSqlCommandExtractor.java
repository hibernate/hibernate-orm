/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;

/**
 * Class responsible for extracting SQL statements from import script. Supports instructions/comments and quoted
 * strings spread over multiple lines. Each statement must end with semicolon.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 *
 * @deprecated Use {@link MultiLineSqlScriptExtractor} instead
 */
@Deprecated
public class MultipleLinesSqlCommandExtractor extends MultiLineSqlScriptExtractor {
}
