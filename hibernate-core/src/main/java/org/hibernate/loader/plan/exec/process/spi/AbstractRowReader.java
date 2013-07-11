/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.exec.process.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.loader.plan.exec.process.internal.CollectionReferenceReader;
import org.hibernate.loader.plan.exec.process.internal.EntityReferenceReader;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan.exec.spi.RowReader;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRowReader implements RowReader {

	@Override
	public Object readRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
		final List<EntityReferenceReader> entityReferenceReaders = getEntityReferenceReaders();
		final List<CollectionReferenceReader> collectionReferenceReaders = getCollectionReferenceReaders();

		final boolean hasEntityReferenceReaders = entityReferenceReaders != null && entityReferenceReaders.size() > 0;
		final boolean hasCollectionReferenceReaders = collectionReferenceReaders != null && collectionReferenceReaders.size() > 0;

		if ( hasEntityReferenceReaders ) {
			// 	1) allow entity references to resolve identifiers (in 2 steps)
			for ( EntityReferenceReader entityReferenceReader : entityReferenceReaders ) {
				entityReferenceReader.hydrateIdentifier( resultSet, context );
			}
			for ( EntityReferenceReader entityReferenceReader : entityReferenceReaders ) {
				entityReferenceReader.resolveEntityKey( resultSet, context );
			}


			// 2) allow entity references to resolve their hydrated state and entity instance
			for ( EntityReferenceReader entityReferenceReader : entityReferenceReaders ) {
				entityReferenceReader.hydrateEntityState( resultSet, context );
			}
		}


		// 3) read the logical row

		Object logicalRow = readLogicalRow( resultSet, context );


		// 4) allow entities and collection to read their elements
		if ( hasEntityReferenceReaders ) {
			for ( EntityReferenceReader entityReferenceReader : entityReferenceReaders ) {
				entityReferenceReader.finishUpRow( resultSet, context );
			}
		}
		if ( hasCollectionReferenceReaders ) {
			for ( CollectionReferenceReader collectionReferenceReader : collectionReferenceReaders ) {
				collectionReferenceReader.finishUpRow( resultSet, context );
			}
		}

		return logicalRow;
	}

	protected abstract List<EntityReferenceReader> getEntityReferenceReaders();
	protected abstract List<CollectionReferenceReader> getCollectionReferenceReaders();

	protected abstract Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context)
			throws SQLException;

}
