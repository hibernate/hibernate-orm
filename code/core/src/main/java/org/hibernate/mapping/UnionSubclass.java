//$Id: UnionSubclass.java 6514 2005-04-26 06:37:54Z oneovthafew $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;

/**
 * A subclass in a table-per-concrete-class mapping
 * @author Gavin King
 */
public class UnionSubclass extends Subclass implements TableOwner {

	private Table table;
	private KeyValue key;

	public UnionSubclass(PersistentClass superclass) {
		super(superclass);
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
		getSuperclass().addSubclassTable(table);
	}

	public java.util.Set getSynchronizedTables() {
		return synchronizedTables;
	}
	
	protected Iterator getNonDuplicatedPropertyIterator() {
		return getPropertyClosureIterator();
	}

	public void validate(Mapping mapping) throws MappingException {
		super.validate(mapping);
		if ( key!=null && !key.isValid(mapping) ) {
			throw new MappingException(
				"subclass key mapping has wrong number of columns: " +
				getEntityName() +
				" type: " +
				key.getType().getName()
			);
		}
	}
	
	public Table getIdentityTable() {
		return getTable();
	}
	
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
}
