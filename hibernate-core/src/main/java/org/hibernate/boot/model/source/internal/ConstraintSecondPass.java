/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.model.source.spi.ConstraintSource;
import org.hibernate.boot.model.source.spi.IndexConstraintSource;
import org.hibernate.boot.model.source.spi.UniqueKeyConstraintSource;
import org.hibernate.cfg.SecondPass;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/**
 * @author Steve Ebersole
 */
public class ConstraintSecondPass implements SecondPass {
	private final MappingDocument sourceDocument;

	private final Table table;
	private final ConstraintSource constraintSource;

	public ConstraintSecondPass(
			MappingDocument sourceDocument,
			Table table,
			ConstraintSource constraintSource) {
		this.sourceDocument = sourceDocument;
		this.table = table;
		this.constraintSource = constraintSource;
	}

	@Override
	public void doSecondPass(Map persistentClasses) throws MappingException {
		if ( IndexConstraintSource.class.isInstance( constraintSource ) ) {
			bindIndexConstraint();
		}
		else if ( UniqueKeyConstraintSource.class.isInstance( constraintSource ) ) {
			bindUniqueKeyConstraint();
		}
	}

	private void bindIndexConstraint() {
		// todo : implicit naming via strategy
		final Index index = table.getOrCreateIndex( constraintSource.name() );

		for ( String columnName : constraintSource.columnNames() ) {
			final Identifier physicalName = sourceDocument.getMetadataCollector().getDatabase().toIdentifier(
					sourceDocument.getMetadataCollector().getPhysicalColumnName( table, columnName )
			);
			index.addColumn( table.getColumn( physicalName ) );
		}
	}

	private void bindUniqueKeyConstraint() {
		// todo : implicit naming via strategy
		final UniqueKey index = table.getOrCreateUniqueKey( constraintSource.name() );

		for ( String columnName : constraintSource.columnNames() ) {
			final Identifier physicalName = sourceDocument.getMetadataCollector().getDatabase().toIdentifier(
					sourceDocument.getMetadataCollector().getPhysicalColumnName( table, columnName )
			);
			index.addColumn( table.getColumn( physicalName ) );
		}
	}
}
