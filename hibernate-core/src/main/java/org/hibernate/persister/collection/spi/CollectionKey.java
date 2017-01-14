/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.collection.spi;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.internal.CollectionElementEntity;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.TypeExporter;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class CollectionKey implements TypeExporter {

	// todo : split into interface/impl
	//		that way we can pass in the impl here, but PluralAttributeKey (interface)
	//		can work with any ImprovedCollectionPersister (interface) impl

	private final AbstractCollectionPersister collectionPersister;

	private List<JoinColumnMapping> joinColumnMappings;

	public CollectionKey(AbstractCollectionPersister collectionPersister) {
		this.collectionPersister = collectionPersister;
	}

	@Override
	public Type getOrmType() {
		return collectionPersister.getOrmType();
	}

	public List<JoinColumnMapping> getJoinColumnMappings() {
		if ( joinColumnMappings == null ) {
			joinColumnMappings = collectionPersister.getAttributeContainer().resolveJoinColumnMappings(
					collectionPersister );
		}
		return joinColumnMappings;
	}

	public List<JoinColumnMapping> buildJoinColumnMappings(List<Column> joinTargetColumns) {
		// NOTE : called from JoinableAttributeContainer#resolveColumnMappings do not "refactor" into #getJoinColumnMappings()

		// NOTE : joinTargetColumns are the owner's columns we join to (target) whereas #resolveJoinSourceColumns()
		//		returns the collection's key columns (join/fk source).
		// todo : would much rather carry forward the ForeignKey (in some "resolved" form from the mapping model
		//		Same for entity-typed attributes (all JoinableAttributes)

		final List<Column> joinSourceColumns = resolveJoinSourceColumns( joinTargetColumns );

		if ( joinSourceColumns.size() != joinTargetColumns.size() ) {
			throw new HibernateException( "Bad resolution of right-hand and left-hand columns for attribute join : " + collectionPersister );
		}

		final List<JoinColumnMapping> joinColumnMappings = CollectionHelper.arrayList( joinSourceColumns.size() );
		for ( int i = 0; i < joinSourceColumns.size(); i++ ) {
			joinColumnMappings.add(
					new JoinColumnMapping(
							joinSourceColumns.get( i ),
							joinTargetColumns.get( i )
					)
			);
		}

		return joinColumnMappings;
	}

	private List<Column> resolveJoinSourceColumns(List<Column> joinTargetColumns) {
		// 	NOTE : If the elements are one-to-many (no collection table) we'd really need to understand
		//		columns (or formulas) across the entity hierarchy.  For now we assume the persister's
		// 		root table.  columns are conceivably doable already since @Column names a specific table.
		//		Maybe we should add same to @Formula
		//
		//		on the bright side, atm CollectionPersister does not currently support
		//		formulas in its key definition
		final String[] columnNames = ( (Joinable) collectionPersister ).getKeyColumnNames();
		final List<Column> columns = CollectionHelper.arrayList( columnNames.length );

		assert joinTargetColumns.size() == columnNames.length;

		final Table separateCollectionTable = collectionPersister.getSeparateCollectionTable();
		if ( separateCollectionTable != null ) {
			for ( int i = 0; i < columnNames.length; i++ ) {
				columns.add(
						separateCollectionTable.makeColumn(
								columnNames[i],
								joinTargetColumns.get( i ).getJdbcType()
						)
				);
			}
		}
		else {
			// otherwise we just need to resolve the column names in the element table(s) (as the "collection table")
			final EntityPersister elementPersister = ( (CollectionElementEntity) collectionPersister.getElementReference() ).getElementPersister();

			for ( int i = 0; i < columnNames.length; i++ ) {
				// it is conceivable that the column already exists
				//		todo : is the same ^^ true for separateCollectionTable?
				Column column = elementPersister.getRootTable().locateColumn( columnNames[i] );
				if ( column == null ) {
					column = elementPersister.getRootTable().makeColumn(
							columnNames[i],
							joinTargetColumns.get( i ).getJdbcType()
					);
				}
				columns.add( column );
			}
		}

		return columns;
	}
}
