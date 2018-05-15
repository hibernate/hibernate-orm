/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.relational.spi;

/**
 * @author Andrea Boriero
 */
public class AuxiliaryDatabaseObject implements Exportable {
	private final String exportIdentifier;
	private final boolean beforeTablesOnCreation;
	private final String[] sqlCreateStrings;
	private final String[] sqlDropStrings;

	public AuxiliaryDatabaseObject(
			String exportIdentifier,
			boolean beforeTablesOnCreation,
			String[] sqlCreateStrings,
			String[] sqlDropStrings) {
		this.exportIdentifier = exportIdentifier;
		this.beforeTablesOnCreation = beforeTablesOnCreation;
		this.sqlCreateStrings = sqlCreateStrings;
		this.sqlDropStrings = sqlDropStrings;
	}

	public String[] getSqlCreateStrings(){
		return sqlCreateStrings;
	}

	public String[] getSqlDropStrings(){
		return sqlDropStrings;
	}

	public boolean isBeforeTablesOnCreation(){
		return beforeTablesOnCreation;
	}

	@Override
	public String getExportIdentifier() {
		return exportIdentifier;
	}
}
