/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.PostgresUUIDType;

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
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(EntityPersister runtimeRootEntityDescriptor) {
		throw new NotYetImplementedFor6Exception( getClass() );

//		return new LocalTemporaryTableBulkIdStrategy(
//				new IdTableSupportStandardImpl() {
//					@Override
//					public String getCreateIdTableCommand() {
//						return "create temporary  table";
//					}
//
//					@Override
//					public String getDropIdTableCommand() {
//						return "drop table";
//					}
//				},
//				AfterUseAction.DROP,
//				null
//		);
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}
}
