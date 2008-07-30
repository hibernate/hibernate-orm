/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.type.CollectionType;
import org.hibernate.type.TypeFactory;

/**
 * A set with no nullable element columns. It will have a primary key
 * consisting of all table columns (ie. key columns + element columns).
 * @author Gavin King
 */
public class Set extends Collection {

	public void validate(Mapping mapping) throws MappingException {
		super.validate( mapping );
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

	/**
	 * Constructor for Set.
	 * @param owner
	 */
	public Set(PersistentClass owner) {
		super(owner);
	}

	public boolean isSet() {
		return true;
	}

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return TypeFactory.sortedSet( getRole(), getReferencedPropertyName(), isEmbedded(), getComparator() );
		}
		else if ( hasOrder() ) {
			return TypeFactory.orderedSet( getRole(), getReferencedPropertyName(), isEmbedded() );
		}
		else {
			return TypeFactory.set( getRole(), getReferencedPropertyName(), isEmbedded() );
		}
	}

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			PrimaryKey pk = new PrimaryKey();
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
}
