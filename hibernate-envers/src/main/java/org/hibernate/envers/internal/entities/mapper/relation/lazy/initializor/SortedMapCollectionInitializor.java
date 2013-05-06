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
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.SortedMap;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * Initializes SortedMap collection with proper Comparator
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class SortedMapCollectionInitializor extends MapCollectionInitializor<SortedMap> {
	private final Comparator comparator;

	public SortedMapCollectionInitializor(
			AuditConfiguration verCfg,
			AuditReaderImplementor versionsReader,
			RelationQueryGenerator queryGenerator,
			Object primaryKey, Number revision, boolean removed,
			Class<? extends SortedMap> collectionClass,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData, Comparator comparator) {
		super(
				verCfg,
				versionsReader,
				queryGenerator,
				primaryKey,
				revision,
				removed,
				collectionClass,
				elementComponentData,
				indexComponentData
		);
		this.comparator = comparator;
	}

	@Override
	protected SortedMap initializeCollection(int size) {
		if ( comparator == null ) {
			return super.initializeCollection( size );
		}
		try {
			return collectionClass.getConstructor( Comparator.class ).newInstance( comparator );
		}
		catch (InstantiationException e) {
			throw new AuditException( e );
		}
		catch (IllegalAccessException e) {
			throw new AuditException( e );
		}
		catch (NoSuchMethodException e) {
			throw new AuditException( e );
		}
		catch (InvocationTargetException e) {
			throw new AuditException( e );
		}
	}

}
