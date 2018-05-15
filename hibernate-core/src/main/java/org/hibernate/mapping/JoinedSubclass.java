/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * A subclass in a table-per-subclass mapping
 * @author Gavin King
 */
public class JoinedSubclass extends Subclass implements TableOwner {
	private MappedTable table;
	private KeyValue key;

	public JoinedSubclass(
			EntityMapping superclass,
			MetadataBuildingContext metadataBuildingContext) {
		super( superclass, metadataBuildingContext );
	}

	@Override
	public Table getTable() {
		return (Table) getMappedTable();
	}

	@Override
	public MappedTable getMappedTable() {
		return table;
	}

	@Override
	public void setMappedTable(MappedTable table) {
		this.table = table;
		getSuperclass().addSubclassTable( table );
	}

	@Override
	public KeyValue getKey() {
		return key;
	}

	public void setKey(KeyValue key) {
		this.key = key;
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
	public Iterator getReferenceablePropertyIterator() {
		return getPropertyIterator();
	}

	@Override
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
}
