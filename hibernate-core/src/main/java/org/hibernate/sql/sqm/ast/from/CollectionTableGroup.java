/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.from;


import org.hibernate.persister.collection.spi.ImprovedCollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.ImprovedEntityPersister;

/**
 * A TableSpecificationGroup for a collection reference
 *
 * @author Steve Ebersole
 */
public class CollectionTableGroup extends AbstractTableGroup {
	private final ImprovedCollectionPersister persister;

	public CollectionTableGroup(
			TableSpace tableSpace,
			String aliasBase,
			ImprovedCollectionPersister persister) {
		super( tableSpace, aliasBase );
		this.persister = persister;
	}

	public ImprovedCollectionPersister getPersister() {
		return persister;
	}

	public ColumnBinding[] resolveKeyColumnBindings() {
		final Column[] columns = persister.getForeignKeyDescriptor().getForeignKeyColumns();

		final TableBinding tableBinding = getRootTableBinding();
		final ColumnBinding[] bindings = new ColumnBinding[columns.length];
		for ( int i = 0; i < columns.length; i++ ) {
			bindings[i] = new ColumnBinding( columns[i], tableBinding );
		}
		return bindings;
	}

	@Override
	protected ImprovedEntityPersister resolveEntityReferenceBase() {
		return (ImprovedEntityPersister) persister.getElementDescriptor().getSqmType();
	}
}
