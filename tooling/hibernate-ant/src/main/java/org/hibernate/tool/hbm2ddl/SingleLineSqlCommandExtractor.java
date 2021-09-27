/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
