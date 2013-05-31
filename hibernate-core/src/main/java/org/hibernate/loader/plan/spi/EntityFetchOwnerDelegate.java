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
import java.util.HashMap;
import java.util.Map;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * A delegate for an entity fetch owner to obtain details about
 * an owned attribute fetch.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class EntityFetchOwnerDelegate extends AbstractFetchOwnerDelegate implements FetchOwnerDelegate {
	private final EntityPersister entityPersister;

	/**
	 * Constructs an {@link EntityFetchOwnerDelegate}.
	 *
	 * @param entityPersister - the entity persister.
	 */
	public EntityFetchOwnerDelegate(EntityPersister entityPersister) {
		this.entityPersister = entityPersister;
	}

	@Override
	protected FetchMetadata buildFetchMetadata(Fetch fetch) {
		final Integer propertyIndex = entityPersister.getEntityMetamodel().getPropertyIndexOrNull(
				fetch.getOwnerPropertyName()
		);
		if ( propertyIndex == null ) {
			// possibly it is part of the identifier; but that's only possible if the identifier is composite
			final Type idType = entityPersister.getIdentifierType();
			if ( CompositeType.class.isInstance( idType ) ) {
				final CompositeType cidType = (CompositeType) idType;
				if ( entityPersister.hasIdentifierProperty() ) {
					// encapsulated composite id case; this *should have* been handled as part of building the fetch...
					throw new WalkingException(
							"Expecting different FetchOwnerDelegate type for encapsulated composite id case"
					);
				}
				else {
					// non-encapsulated composite id case...
					return new NonEncapsulatedIdentifierAttributeFetchMetadata(
							entityPersister,
							cidType,
							fetch.getOwnerPropertyName()
					);
				}
			}
		}
		else {
			return new NonIdentifierAttributeFetchMetadata(
					entityPersister,
					fetch.getOwnerPropertyName(),
					propertyIndex.intValue()
			);
		}

		throw new WalkingException(
				"Could not locate metadata about given fetch [" + fetch + "] in its owning persister"
		);
	}

	private class NonIdentifierAttributeFetchMetadata implements FetchMetadata {
		private final EntityPersister entityPersister;
		private final String attributeName;
		private final int propertyIndex;

		private Type attributeType;

		public NonIdentifierAttributeFetchMetadata(
				EntityPersister entityPersister,
				String attributeName,
				int propertyIndex) {
			this.entityPersister = entityPersister;
			this.attributeName = attributeName;
			this.propertyIndex = propertyIndex;
		}

		@Override
		public boolean isNullable() {
			return entityPersister.getPropertyNullability()[ propertyIndex ];
		}

		@Override
		public Type getType() {
			if ( attributeType == null ) {
				attributeType = entityPersister.getPropertyTypes()[ propertyIndex ];
			}
			return attributeType;
		}

		@Override
		public String[] toSqlSelectFragments(String alias) {
//			final Type type = getType();
//			final OuterJoinLoadable outerJoinLoadable = (OuterJoinLoadable) entityPersister;
			final Queryable queryable = (Queryable) entityPersister;

			return queryable.toColumns( alias, attributeName );
//			if ( type.isAssociationType() ) {
//				return JoinHelper.getLHSColumnNames(
//						(AssociationType) type,
//						propertyIndex,
//						outerJoinLoadable,
//						outerJoinLoadable.getFactory()
//				);
//			}
//			else {
//				return outerJoinLoadable.getPropertyColumnNames( propertyIndex );
//			}
		}
	}

	private class NonEncapsulatedIdentifierAttributeFetchMetadata implements FetchMetadata {
		private final EntityPersister entityPersister;
		private final String attributeName;

		// virtually final fields
		private Type type;
		private int selectFragmentRangeStart;
		private int selectFragmentRangeEnd;


		public NonEncapsulatedIdentifierAttributeFetchMetadata(
				EntityPersister entityPersister,
				CompositeType cidType,
				String attributeName) {
			this.entityPersister = entityPersister;
			this.attributeName = attributeName;

			this.selectFragmentRangeStart = 0;
			Type subType;
			boolean foundIt = false;
			for ( int i = 0; i < cidType.getPropertyNames().length; i++ ) {
				subType = cidType.getSubtypes()[i];
				if ( cidType.getPropertyNames()[i].equals( attributeName ) ) {
					// found it!
					foundIt = true;
					this.type = subType;
					break;
				}
				selectFragmentRangeStart += subType.getColumnSpan( entityPersister.getFactory() );
			}

			if ( !foundIt ) {
				throw new WalkingException( "Could not find " );
			}

			selectFragmentRangeEnd = selectFragmentRangeStart + type.getColumnSpan( entityPersister.getFactory() );
		}

		@Override
		public boolean isNullable() {
			return false;
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public String[] toSqlSelectFragments(String alias) {
			return Arrays.copyOfRange(
					( (Queryable) entityPersister ).toColumns( alias, attributeName ),
					selectFragmentRangeStart,
					selectFragmentRangeEnd
			);
		}
	}
}
