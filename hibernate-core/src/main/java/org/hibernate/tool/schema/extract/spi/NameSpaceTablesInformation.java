/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;

/**
 * @author Andrea Boriero
 */
public class NameSpaceTablesInformation {
	private final IdentifierHelper identifierHelper;
	private Map<String, TableInformation> tables = new HashMap<>();

	public NameSpaceTablesInformation(IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addTableInformation(TableInformation tableInformation) {
		tables.put( tableInformation.getName().getTableName().getText(), tableInformation );
	}

	public TableInformation getTableInformation(ExportableTable table) {
		return tables.get( identifierHelper.toMetaDataObjectName( table.getTableName() ) );
	}

	public TableInformation getTableInformation(String tableName) {
		return tables.get( tableName );
	}
}
