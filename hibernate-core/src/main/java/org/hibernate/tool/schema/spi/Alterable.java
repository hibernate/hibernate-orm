/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.Exportable;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * Defines a contract for exporting of database objects (tables, etc) for use in SQL {@code ALTER} scripts.
 * <p/>
 * This is an ORM-centric contract
 *
 * @author Andrea Boriero
 */
public interface Alterable<T extends Exportable> {
	/**
	 * Get the commands needed for alter.
	 *
	 * @return The commands needed for alter scripting.
	 */
	String[] getSqlAlterStrings(T exportable, TableInformation tableInfo, JdbcServices jdbcServices);
}
