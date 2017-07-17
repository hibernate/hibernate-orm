/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.service.ServiceRegistry;

/**
 * An SQL dialect for Postgres 8.2 and later, adds support for "if exists" when dropping tables
 * 
 * @author edalquist
 */
public class PostgreSQL82Dialect extends PostgreSQL81Dialect {
	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		// HHH-9562
		typeContributions.contributeType( PostgresUUIDType.INSTANCE );
	}

	@Override
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy(
				new StandardIdTableSupport(
						new LocalTempTableExporter() {
							@Override
							public String getCreateCommand() {
								return "create temporary  table";
							}
						}
				)
		);
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}
}
