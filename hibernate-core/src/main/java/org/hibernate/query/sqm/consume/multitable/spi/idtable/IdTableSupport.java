/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.naming.Identifier;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Defines environment's support for id-tables.  Generally this comes from
 * Dialect.
 *
 * todo (6.0) : get rid of this contract - the strategy can handle this on its own
 * 		this is a left-over from porting pre-6.0 support
 *
 * @author Steve Ebersole
 */
public interface IdTableSupport {
	/**
	 * Determine the name to use for the id-table.
	 */
	Identifier determineIdTableName(EntityTypeDescriptor entityDescriptor, SessionFactoryOptions sessionFactoryOptions);

	Exporter<IdTable> getIdTableExporter();

	default IdTableManagementTransactionality geIdTableManagementTransactionality(){
		return null;
	}
}
