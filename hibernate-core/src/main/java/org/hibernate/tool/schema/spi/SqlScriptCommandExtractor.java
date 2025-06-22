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
 *
 * @author Lukasz Antoniak
 *
 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_IMPORT_FILES
 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE
 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE
 */
public interface SqlScriptCommandExtractor extends Service {
	/**
	 * Read the commands from the SQL script represented by the incoming reader, returning
	 * those commands as an array
	 */
	List<String> extractCommands(Reader reader, Dialect dialect);
}
