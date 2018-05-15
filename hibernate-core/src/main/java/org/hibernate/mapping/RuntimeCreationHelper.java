/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public class RuntimeCreationHelper {
	private RuntimeCreationHelper() {
	}

	public static ForeignKey generateForeignKey(
			RuntimeModelCreationContext creationContext,
			org.hibernate.mapping.ForeignKey bootFk) {
		final Table runtimeReferencingTable = creationContext.getDatabaseObjectResolver().resolveTable( bootFk.getReferencedTable() );
		final Table runtimeTargetTable = creationContext.getDatabaseObjectResolver().resolveTable( bootFk.getTargetTable() );

		final ForeignKey.Builder runtimeFkBuilder = new ForeignKey.Builder(
				bootFk.getName(),
				bootFk.isCreationEnabled(),
				bootFk.isReferenceToPrimaryKey(),
				bootFk.isCascadeDeleteEnabled(),
				bootFk.getKeyDefinition(),
				runtimeReferencingTable,
				runtimeTargetTable
		);

		final Iterator<MappedColumn> bootTargetColumnItr = bootFk.getTargetColumns().iterator();
		for ( MappedColumn bootReferencedColumn :  bootFk.getReferencedColumns() ) {
			assert bootTargetColumnItr.hasNext();

			final MappedColumn bootTargetColumn = bootTargetColumnItr.next();
			runtimeFkBuilder.addColumnMapping(
					creationContext.getDatabaseObjectResolver().resolveColumn( bootReferencedColumn ),
					creationContext.getDatabaseObjectResolver().resolveColumn( bootTargetColumn )
			);
		}

		return runtimeFkBuilder.build();
	}
}
