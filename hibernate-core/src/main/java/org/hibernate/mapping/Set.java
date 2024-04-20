/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.CollectionType;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedSetType;
import org.hibernate.usertype.UserCollectionType;

/**
 * A mapping model object representing a collection of type {@link java.util.List}.
 * A set has no nullable element columns (unless it is a one-to-many association).
 * It has a primary key consisting of all columns (i.e. key columns + element columns).
 *
 * @author Gavin King
 */
public class Set extends Collection {
	/**
	 * Used by hbm.xml binding
	 */
	public Set(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	/**
	 * Used by annotation binding
	 */
	public Set(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass persistentClass, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, persistentClass, buildingContext );
	}

	private Set(Collection original) {
		super( original );
	}

	@Override
	public Set copy() {
		return new Set( this );
	}

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

	public boolean isSet() {
		return true;
	}

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return new SortedSetType( getRole(), getReferencedPropertyName(), getComparator() );
		}

		if ( hasOrder() ) {
			return new OrderedSetType( getRole(), getReferencedPropertyName() );
		}

		return new SetType( getRole(), getReferencedPropertyName() );
	}

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			final Table collectionTable = getCollectionTable();
			PrimaryKey pk = collectionTable.getPrimaryKey();
			if ( pk == null ) {
				pk = new PrimaryKey( getCollectionTable() );
				pk.addColumns( getKey() );
				for ( Selectable selectable : getElement().getSelectables() ) {
					if ( selectable instanceof Column ) {
						Column col = (Column) selectable;
						if ( !col.isNullable() ) {
							pk.addColumn( col );
						}
						else {
							return;
						}
					}
				}
				if ( pk.getColumnSpan() != getKey().getColumnSpan() ) {
					collectionTable.setPrimaryKey( pk );
				}
//				else {
					//for backward compatibility, allow a set with no not-null
					//element columns, using all columns in the row locator SQL
					//TODO: create an implicit not null constraint on all cols?
//				}
			}
		}
//		else {
			//create an index on the key columns??
//		}
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
