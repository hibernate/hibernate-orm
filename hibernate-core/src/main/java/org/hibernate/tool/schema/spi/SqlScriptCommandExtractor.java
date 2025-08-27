/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.io.Reader;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;

/**
 * Contract for extracting statements from source/import/init scripts.
 * <p>
 * A concrete implementation may be selected via
 * {@value org.hibernate.cfg.SchemaToolingSettings#HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR}.
 *
 * @author Lukasz Antoniak
 *
 * @see org.hibernate.cfg.SchemaToolingSettings#HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR
 * @see org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor
 * @see org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor
 *
 * @see org.hibernate.cfg.SchemaToolingSettings#HBM2DDL_IMPORT_FILES
 * @see org.hibernate.cfg.SchemaToolingSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
 * @see org.hibernate.cfg.SchemaToolingSettings#JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE
 * @see org.hibernate.cfg.SchemaToolingSettings#JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE
 */
public interface SqlScriptCommandExtractor extends Service {
	/**
	 * Read the commands from the SQL script represented by the incoming reader, returning
	 * those commands as an array
	 */
	List<String> extractCommands(Reader reader, Dialect dialect);
}
