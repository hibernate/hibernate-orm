/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Defines environment's support for id-tables.  Generally this comes from
 * Dialect.
 *
 * @author Steve Ebersole
 */
public interface IdTableSupport {
	/**
	 * Determine the name to use for the id-table.
	 */
	QualifiedTableName determineIdTableName(
			EntityDescriptor entityDescriptor,
			JdbcEnvironment jdbcEnvironment,
			Identifier catalog,
			Identifier schema);

	Exporter<IdTable> getIdTableExporter();
}
