/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.Metamodel;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableStrategy extends AbstractTableBasedStrategy {
	public static final String SHORT_NAME = "local_temporary";

	private final IdTableSupport idTableSupport;
	private IdTableManagementTransactionality transactionality;

	public LocalTemporaryTableStrategy() {
		this( generateStandardIdTableSupport() );
	}

	private static StandardIdTableSupport generateStandardIdTableSupport() {
		return new StandardIdTableSupport( generateStandardExporter() );
	}

	private static Exporter<IdTable> generateStandardExporter() {
		return new IdTableExporterImpl() {
			@Override
			protected String getCreateCommand() {
				return "create local temporary table";
			}
		};
	}

	public LocalTemporaryTableStrategy(IdTableSupport idTableSupport) {
		this( idTableSupport, null );
	}

	public LocalTemporaryTableStrategy(IdTableManagementTransactionality transactionality) {
		this( generateStandardIdTableSupport(), transactionality );
	}

	public LocalTemporaryTableStrategy(IdTableSupport idTableSupport, IdTableManagementTransactionality transactionality) {
		this.idTableSupport = idTableSupport;
		this.transactionality = transactionality;
	}

	@Override
	protected IdTableSupport getIdTableSupport() {
		return idTableSupport;
	}

	@Override
	public BeforeUseAction getBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}

	@Override
	public AfterUseAction getAfterUseAction() {
		return AfterUseAction.DROP;
	}

	@Override
	public IdTableManagementTransactionality getTableManagementTransactionality() {
		return transactionality;
	}

	@Override
	public void prepare(
			Metamodel runtimeMetadata,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {
		super.prepare( runtimeMetadata, sessionFactoryOptions, connectionAccess );

		if ( transactionality == null ) {
			// see if the user explicitly requested one via settings...
			switch ( sessionFactoryOptions.getTempTableDdlTransactionHandling() ) {
				case ISOLATE: {
					transactionality = IdTableManagementTransactionality.ISOLATE;
				}
				case ISOLATE_AND_TRANSACT: {
					transactionality = IdTableManagementTransactionality.ISOLATE_AND_TRANSACT;
				}
				default: {
					// nothing to do - null will be handled properly by super
				}
			}
		}
	}

	@Override
	protected NamespaceHandling getNamespaceHandling() {
		// by default local temp tables will use no namespace
		return NamespaceHandling.USE_NONE;
	}
}
