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
package org.hibernate.loader.plan.spi;

import java.util.Arrays;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Gail Badner
 */
public class CompositeFetchOwnerDelegate implements FetchOwnerDelegate {
	private final SessionFactoryImplementor sessionFactory;
	private final CompositeType compositeType;
	private final String[] columnNames;

	public CompositeFetchOwnerDelegate(
			SessionFactoryImplementor sessionFactory,
			CompositeType compositeType,
			String[] columnNames) {
		this.sessionFactory = sessionFactory;
		this.compositeType = compositeType;
		this.columnNames = columnNames;
	}

	@Override
	public boolean isNullable(Fetch fetch) {
		return compositeType.getPropertyNullability()[ determinePropertyIndex( fetch ) ];
	}

	@Override
	public Type getType(Fetch fetch) {
		return compositeType.getSubtypes()[ determinePropertyIndex( fetch ) ];
	}

	@Override
	public String[] getColumnNames(Fetch fetch) {
		// TODO: probably want to cache this
		int begin = 0;
		String[] subColumnNames = null;
		for ( int i = 0; i < compositeType.getSubtypes().length; i++ ) {
			final int columnSpan = compositeType.getSubtypes()[i].getColumnSpan( sessionFactory );
			subColumnNames = ArrayHelper.slice( columnNames, begin, columnSpan );
			if ( compositeType.getPropertyNames()[ i ].equals( fetch.getOwnerPropertyName() ) ) {
				break;
			}
			begin += columnSpan;
		}
		return subColumnNames;
	}

	private int determinePropertyIndex(Fetch fetch) {
		// TODO: probably want to cache this
		final String[] subAttributeNames = compositeType.getPropertyNames();
		int subAttributeIndex = -1;
		for ( int i = 0; i < subAttributeNames.length ; i++ ) {
			if ( subAttributeNames[ i ].equals( fetch.getOwnerPropertyName() ) ) {
				subAttributeIndex = i;
				break;
			}
		}
		if ( subAttributeIndex == -1 ) {
			throw new WalkingException(
					String.format(
							"Owner property [%s] not found in composite properties [%s]",
							fetch.getOwnerPropertyName(),
							Arrays.asList( subAttributeNames )
					)
			);
		}
		return subAttributeIndex;
	}
}
