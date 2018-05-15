/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;

/**
 * A subclass in a table-per-concrete-class mapping
 * @author Gavin King
 */
public class UnionSubclass extends Subclass implements TableOwner {
	private MappedTable table;
	private KeyValue key;

	public UnionSubclass(
			PersistentClass superclass,
			MetadataBuildingContext metadataBuildingContext) {
		super( superclass, metadataBuildingContext );
	}

	@Override
	public Table getTable() {
		return (Table) table;
	}

	@Override
	public MappedTable getMappedTable() {
		return table;
	}

	@Override
	public void setMappedTable(MappedTable table) {
		this.table = table;
		getSuperclass().addSubclassTable(table);
	}

	@Override
	public java.util.Set getSynchronizedTables() {
		return synchronizedTables;
	}

	@Override
	protected Iterator getNonDuplicatedPropertyIterator() {
		return getPropertyClosureIterator();
	}

	@Override
	public void validate() throws MappingException {
		super.validate();
		if ( key!=null && !key.isValid() ) {
			throw new MappingException(
				"subclass key mapping has wrong number of columns: " +
				getEntityName() +
				" type: " +
				key.getJavaTypeMapping().getTypeName()
			);
		}
	}

	@Override
	public MappedTable getIdentityTable() {
		return getMappedTable();
	}

	@Override
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}

	@Override
	public ExecuteUpdateResultCheckStyle getUpdateResultCheckStyle(){
		String sql = getCustomSQLUpdate();
		boolean callable = sql != null && isCustomUpdateCallable();
		ExecuteUpdateResultCheckStyle checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: getCustomSQLUpdateCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: getCustomSQLUpdateCheckStyle();
		return checkStyle;
	}
}
