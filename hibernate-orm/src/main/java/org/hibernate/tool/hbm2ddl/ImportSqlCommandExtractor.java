/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.Reader;

import org.hibernate.service.Service;

/**
 * Contract for extracting statements from source/import/init scripts.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 *
 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_IMPORT_FILES
 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_LOAD_SCRIPT_SOURCE
 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_CREATE_SCRIPT_SOURCE
 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DROP_SCRIPT_SOURCE
 */
public interface ImportSqlCommandExtractor extends Service {
	/**
	 * @param reader Character stream reader of SQL script.
	 * @return List of single SQL statements. Each command may or may not contain semicolon at the end.
	 */
	String[] extractCommands(Reader reader);
}
