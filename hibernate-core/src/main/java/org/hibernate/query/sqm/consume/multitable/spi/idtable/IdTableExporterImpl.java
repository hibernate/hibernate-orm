/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class IdTableExporterImpl implements Exporter<IdTable> {
	protected String getCreateCommand() {
		return "create table";
	}

	protected String getCreateOptions() {
		return null;
	}

	protected String getDropCommand() {
		return "drop table";
	}

	protected String getTruncateIdTableCommand(){
		return "delete from";
	}

	@Override
	public String[] getSqlCreateStrings(IdTable exportable, JdbcServices jdbcServices) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final StringBuilder buffer = new StringBuilder( getCreateCommand() ).append( ' ' );
		buffer.append( determineIdTableNameForCreate( exportable, jdbcServices ) );
		buffer.append( '(' );

		boolean firstPass = true;
		for ( PhysicalColumn column : exportable.getPhysicalColumns() ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				buffer.append( ", " );
			}

			buffer.append( column.getName().render( dialect ) ).append( ' ' );
			buffer.append( column.getSqlTypeName() );
			// id values cannot be null
			buffer.append( " not null" );
		}

		buffer.append( ") " );

		final String createOptions = getCreateOptions();
		if ( createOptions != null ) {
			buffer.append( createOptions );
		}

		return new String[] { buffer.toString() };
	}

	protected String determineIdTableNameForCreate(IdTable exportable, JdbcServices jdbcServices) {
		return determineIdTableName( exportable, jdbcServices );
	}

	protected String determineIdTableName(IdTable exportable, JdbcServices jdbcServices) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();
		return jdbcEnvironment.getQualifiedObjectNameFormatter()
				.format( exportable.getQualifiedTableName(), dialect );
	}

	@Override
	public String[] getSqlDropStrings(IdTable exportable, JdbcServices jdbcServices) {
		return new String[] {
				getDropCommand() + ' ' + determineIdTableNameForDrop( exportable, jdbcServices )
		};
	}

	private String determineIdTableNameForDrop(IdTable exportable, JdbcServices jdbcServices) {
		return determineIdTableName( exportable, jdbcServices );
	}
}
