/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor;

/**
 * Class responsible for extracting SQL statements from import script. Treats each line as a complete SQL statement.
 * Comment lines shall start with {@code --}, {@code //} or {@code /*} character sequence.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 *
 * @see SingleLineSqlScriptExtractor
 *
 * @deprecated Use {@link SingleLineSqlScriptExtractor} instead
 */
@Deprecated
public class SingleLineSqlCommandExtractor extends SingleLineSqlScriptExtractor {
}
