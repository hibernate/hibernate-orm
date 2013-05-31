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
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * A delegate for a composite fetch owner to obtain details about an
 * owned sub-attribute fetch.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class CompositeFetchOwnerDelegate extends AbstractFetchOwnerDelegate implements FetchOwnerDelegate {
	private final SessionFactoryImplementor sessionFactory;
	private final CompositeType compositeType;
	private final PropertyMappingDelegate propertyMappingDelegate;

	/**
	 * Constructs a CompositeFetchOwnerDelegate
	 *
	 * @param sessionFactory - the session factory.
	 * @param compositeType - the composite type.
	 * @param propertyMappingDelegate - delegate for handling property mapping
	 */
	public CompositeFetchOwnerDelegate(
			SessionFactoryImplementor sessionFactory,
			CompositeType compositeType,
			PropertyMappingDelegate propertyMappingDelegate) {
		this.sessionFactory = sessionFactory;
		this.compositeType = compositeType;
		this.propertyMappingDelegate = propertyMappingDelegate;
	}

	public static interface PropertyMappingDelegate {
		public String[] toSqlSelectFragments(String alias);
	}

	@Override
	protected FetchMetadata buildFetchMetadata(Fetch fetch) {
		int subIndex = -1;
		int selectFragmentRangeStart = 0;
		int selectFragmentRangeEnd = -1;

		for ( int i = 0; i < compositeType.getPropertyNames().length; i++ ) {
			final Type type = compositeType.getSubtypes()[i];
			final int typeColSpan = type.getColumnSpan( sessionFactory );
			if ( compositeType.getPropertyNames()[ i ].equals( fetch.getOwnerPropertyName() ) ) {
				// fount it!
				subIndex = i;
				selectFragmentRangeEnd = selectFragmentRangeStart + typeColSpan;
				break;
			}
			selectFragmentRangeStart += typeColSpan;
		}

		if ( subIndex < 0 ) {
			throw new WalkingException(
					String.format(
							"Owner property [%s] not found in composite properties [%s]",
							fetch.getOwnerPropertyName(),
							Arrays.asList( compositeType.getPropertyNames() )
					)
			);
		}

		return new FetchMetadataImpl(
				compositeType,
				subIndex,
				propertyMappingDelegate,
				selectFragmentRangeStart,
				selectFragmentRangeEnd
		);

		// todo : we really need a PropertyMapping delegate which can encapsulate both the PropertyMapping and the path
	}

	private static class FetchMetadataImpl implements FetchMetadata {
		private final CompositeType compositeType;
		private final int index;
		private final PropertyMappingDelegate propertyMappingDelegate;
		private final int selectFragmentRangeStart;
		private final int selectFragmentRangeEnd;

		public FetchMetadataImpl(
				CompositeType compositeType,
				int index,
				PropertyMappingDelegate propertyMappingDelegate,
				int selectFragmentRangeStart,
				int selectFragmentRangeEnd) {
			this.compositeType = compositeType;
			this.index = index;
			this.propertyMappingDelegate = propertyMappingDelegate;
			this.selectFragmentRangeStart = selectFragmentRangeStart;
			this.selectFragmentRangeEnd = selectFragmentRangeEnd;
		}

		@Override
		public boolean isNullable() {
			return compositeType.getPropertyNullability()[ index ];
		}

		@Override
		public Type getType() {
			return compositeType.getSubtypes()[ index ];
		}

		@Override
		public String[] toSqlSelectFragments(String alias) {
			return Arrays.copyOfRange(
					propertyMappingDelegate.toSqlSelectFragments( alias ),
					selectFragmentRangeStart,
					selectFragmentRangeEnd
			);
		}
	}
}
