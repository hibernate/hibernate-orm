/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.naming.Identifier;
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
		return determineIdTableName( entityDescriptor );
	}

	protected Identifier determineIdTableName(EntityTypeDescriptor entityDescriptor) {
		final Identifier idTableNameBase = determineIdTableNameBase( entityDescriptor );
		return determineIdTableName( idTableNameBase );
	}

	private Identifier determineIdTableNameBase(EntityTypeDescriptor entityDescriptor) {
		if ( entityDescriptor.getPrimaryTable() instanceof PhysicalTable ) {
			return ( (PhysicalTable) entityDescriptor.getPrimaryTable() ).getTableName();
		}
		else {
			return Identifier.toIdentifier( entityDescriptor.getJpaEntityName() );
		}
	}

	protected Identifier determineIdTableName(Identifier baseName) {
		return new Identifier( "HT_" + baseName.getText(), false );
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
