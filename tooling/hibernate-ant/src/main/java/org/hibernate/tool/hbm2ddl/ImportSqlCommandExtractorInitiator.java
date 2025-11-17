/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
