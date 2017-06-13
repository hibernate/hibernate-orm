/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.internal.SetJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * A set with no nullable element columns. It will have a primary key
 * consisting of all table columns (ie. key columns + element columns).
 * @author Gavin King
 */
public class Set extends Collection {
	public Set(MetadataBuildingContext context, PersistentClass owner) {
		super( context, owner );
	}

	public void validate() throws MappingException {
		super.validate();
		//for backward compatibility, disable this:
		/*Iterator iter = getElement().getColumnIterator();
		while ( iter.hasNext() ) {
			Column col = (Column) iter.next();
			if ( !col.isNullable() ) {
				return;
			}
		}
		throw new MappingException("set element mappings must have at least one non-nullable column: " + getRole() );*/
	}

	public boolean isSet() {
		return true;
	}

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			MappedPrimaryKey pk = new MappedPrimaryKey( getCollectionTable() );
			pk.addColumns( getKey().getColumnIterator() );
			Iterator iter = getElement().getColumnIterator();
			while ( iter.hasNext() ) {
				Object selectable = iter.next();
				if ( selectable instanceof Column ) {
					Column col = (Column) selectable;
					if ( !col.isNullable() ) {
						pk.addColumn(col);
					}
				}
			}
			if ( pk.getColumnSpan()==getKey().getColumnSpan() ) { 
				//for backward compatibility, allow a set with no not-null 
				//element columns, using all columns in the row locater SQL
				//TODO: create an implicit not null constraint on all cols?
			}
			else {
				getCollectionTable().setPrimaryKey(pk);
			}
		}
		else {
			//create an index on the key columns??
		}
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return SetJavaDescriptor.INSTANCE;
	}
}
