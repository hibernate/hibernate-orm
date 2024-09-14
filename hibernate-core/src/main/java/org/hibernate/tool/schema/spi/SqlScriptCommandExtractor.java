/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
