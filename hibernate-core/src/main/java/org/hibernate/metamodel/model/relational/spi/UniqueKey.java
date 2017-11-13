/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.naming.Identifier;

/**
 * @author Andrea Boriero
 */
public class UniqueKey implements Exportable {
	private Table table;
	private Identifier name;
	private List<PhysicalColumn> columns = new ArrayList<>(  );
	private java.util.Map<PhysicalColumn, String> columnOrderMap = new HashMap<>();

	public UniqueKey(Identifier name, Table table) {
		this.name = name;
		this.table = table;
	}

	public Identifier getName() {
		return name;
	}

	public Table getTable() {
		return table;
	}

	public Collection<PhysicalColumn> getColumns(){
		return columns;
	}


	public Map<PhysicalColumn, String> getColumnOrderMap() {
		return columnOrderMap;
	}

	public void addColumn(PhysicalColumn column, String order){
		columns.add( column );
		if ( order != null && !"".equals( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( ( (ExportableTable) getTable() ).getTableName().getText(), "IDX-" + getName().getText() );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		UniqueKey uniqueKey = (UniqueKey) o;
		return Objects.equals( table, uniqueKey.table ) &&
				Objects.equals( name, uniqueKey.name );
	}

	@Override
	public int hashCode() {

		return Objects.hash( table, name );
	}
}
