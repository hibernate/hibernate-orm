/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.naming.Identifier;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableManagementTransactionality;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableSupport;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardIdTableSupport implements IdTableSupport {
	private final Exporter<IdTable> idTableExporter;
	private IdTableManagementTransactionality tableManagementTransactionality;

	public StandardIdTableSupport(Exporter<IdTable> idTableExporter) {
		this.idTableExporter = idTableExporter;
	}

	public StandardIdTableSupport(Exporter<IdTable> idTableExporter, IdTableManagementTransactionality tableManagementTransactionality) {
		this(idTableExporter);
		this.tableManagementTransactionality = tableManagementTransactionality;
	}

	@Override
	public Identifier determineIdTableName(EntityTypeDescriptor entityDescriptor, SessionFactoryOptions sessionFactoryOptions) {
		return determineIdTableName(
				entityDescriptor,
				sessionFactoryOptions.getServiceRegistry().getService( JdbcServices.class ).getJdbcEnvironment()
		);
	}

	protected Identifier determineIdTableName(
			EntityTypeDescriptor entityDescriptor,
			JdbcEnvironment jdbcEnvironment) {
		final String idTableNameBase = determineIdTableNameBase( entityDescriptor, jdbcEnvironment );
		final String idTableName = determineIdTableName( idTableNameBase );

		return jdbcEnvironment.getIdentifierHelper().toIdentifier( idTableName );
	}

	private String determineIdTableNameBase(EntityTypeDescriptor entityDescriptor, JdbcEnvironment jdbcEnvironment) {
		if ( entityDescriptor.getPrimaryTable() instanceof PhysicalTable ) {
			return ( (PhysicalTable) entityDescriptor.getPrimaryTable() ).getTableName().render( jdbcEnvironment.getDialect() );
		}
		else {
			return entityDescriptor.getJpaEntityName();
		}
	}

	protected String determineIdTableName(String baseName) {
		return "HT_" + baseName;
	}

	@Override
	public Exporter<IdTable> getIdTableExporter() {
		return idTableExporter;
	}

	@Override
	public IdTableManagementTransactionality geIdTableManagementTransactionality() {
		return tableManagementTransactionality;
	}
}
