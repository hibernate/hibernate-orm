/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.naming.Identifier;

/**
 * @author Andrea Boriero
 */
public class Index implements Exportable {
	private final ExportableTable table;
	private final List<PhysicalColumn> columns = new ArrayList<>();
	private final Identifier name;


	public Index(Identifier name, ExportableTable table) {
		this.name = name;
		this.table = table;
	}

	public void addColumn(PhysicalColumn column) {
		columns.add( column );
	}

	public Identifier getName() {
		return name;
	}

	public List<PhysicalColumn> getColumns() {
		return columns;
	}

	public ExportableTable getTable() {
		return table;
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getTableName().getText(), "IDX-" + getName().render() );
	}
}
