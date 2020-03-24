/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.tool.schema.internal.script.SqlScriptExtractorInitiator;

/**
 * Instantiates and configures an appropriate {@link ImportSqlCommandExtractor}. By default
 * {@link SingleLineSqlCommandExtractor} is used.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 *
 * @deprecated User {@link SqlScriptExtractorInitiator} instead
 */
@Deprecated
public class ImportSqlCommandExtractorInitiator extends SqlScriptExtractorInitiator {
}
