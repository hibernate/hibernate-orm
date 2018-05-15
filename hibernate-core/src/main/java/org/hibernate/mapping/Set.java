/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Comparator;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.relational.MappedPrimaryKey;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardOrderedSetSemantics;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.collection.internal.StandardSortedSetSemantics;
import org.hibernate.collection.spi.CollectionSemantics;

/**
 * A set with no nullable element columns. It will have a primary key
 * consisting of all table columns (ie. key columns + element columns).
 * @author Gavin King
 */
public class Set extends Collection {
	private final CollectionJavaTypeMapping javaTypeMapping;

	public Set(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );

		javaTypeMapping = new CollectionJavaTypeMapping(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				java.util.Set.class
		);
	}

	public void validate() throws MappingException {
		super.validate();

//for backward compatibility, disable this:
//		for ( MappedColumn mappedColumn : getElement().getMappedColumns() ) {
//			// assume a formula is nullable
//			if ( mappedColumn instanceof Column && ( (Column) mappedColumn ).isNullable() ) {
//				return;
//			}
//		}
//		throw new MappingException("set element mappings must have at least one non-nullable column: " + getRole() );*/
	}

	public boolean isSet() {
		return true;
	}

	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			final MappedPrimaryKey pk = new PrimaryKey( getMappedTable() );
			pk.addColumns( getKey().getMappedColumns() );
			for ( Object selectable : getElement().getMappedColumns() ) {
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

			if ( pk.getColumnSpan() == getKey().getColumnSpan() ) {
				//for backward compatibility, allow a set with no not-null
				//element columns, using all columns in the row locater SQL
				//TODO: create an implicit not null constraint on all cols?
			}
			else {
				getMappedTable().setPrimaryKey( pk );
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
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}

	@Override
	@SuppressWarnings("unchecked")
	public CollectionSemantics getCollectionSemantics() {
		final Comparator comparator = getComparator();
		if ( comparator != null ) {
			return StandardSortedSetSemantics.INSTANCE;
		}

		if ( hasOrder() ) {
			return StandardOrderedSetSemantics.INSTANCE;
		}

		return StandardSetSemantics.INSTANCE;
	}
}
