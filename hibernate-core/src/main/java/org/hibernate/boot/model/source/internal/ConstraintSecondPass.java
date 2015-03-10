/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
