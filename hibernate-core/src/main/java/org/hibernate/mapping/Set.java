/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.mapping;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.cfg.Mappings;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.type.CollectionType;

/**
 * A set which only has a primary key, if all columns are not nullable. A primary key consists of all table columns (ie.
 * key columns + element columns).
 * @author Gavin King
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class Set extends Collection {

	public void validate(Mapping mapping) throws MappingException {
		super.validate( mapping );
	}

	public Set(Mappings mappings, PersistentClass owner) {
		super( mappings, owner );
	}

	public boolean isSet() {
		return true;
	}

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return getMappings().getTypeResolver()
					.getTypeFactory()
					.sortedSet( getRole(), getReferencedPropertyName(), getComparator() );
		}
		else if ( hasOrder() ) {
			return getMappings().getTypeResolver()
					.getTypeFactory()
					.orderedSet( getRole(), getReferencedPropertyName() );
		}
		else {
			return getMappings().getTypeResolver()
					.getTypeFactory()
					.set( getRole(), getReferencedPropertyName() );
		}
	}

	void createPrimaryKey() {
		if ( !isOneToMany() && !hasNullableColumn() ) {
			PrimaryKey pk = new PrimaryKey();
			pk.addColumns( getKey().getColumnIterator() );
			Iterator<?> iter = getElement().getColumnIterator();
			while ( iter.hasNext() ) {
				Object selectable = iter.next();
				if ( selectable instanceof Column ) {
					Column col = (Column) selectable;
					pk.addColumn(col);
				}
			}
			getCollectionTable().setPrimaryKey(pk);
		}
		else {
			//create an index on the key columns??
		}
	}

	private boolean hasNullableColumn() {
		boolean result = false;
		final Iterator<?> iter = getElement().getColumnIterator();
		while ( iter.hasNext() ) {
			Object selectable = iter.next();
			if ( selectable instanceof Column ) {
				Column col = (Column) selectable;
				if ( col.isNullable() ) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
